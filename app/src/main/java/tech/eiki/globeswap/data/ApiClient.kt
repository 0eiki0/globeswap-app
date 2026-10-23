package tech.eiki.globeswap.data

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Fehlerantwort des Servers, [code] ist z. B. "banned", "rate_limited" oder "not_found". */
class ApiException(val status: Int, val code: String) : IOException("HTTP $status: $code")

/**
 * HTTP-Zugang zum PhotoSwap-Server. Beim ersten Aufruf registriert sich die App anonym
 * und speichert das Token; eine Anmeldung durch den Nutzer gibt es nicht.
 */
class ApiClient(
    context: Context,
    private val baseUrl: String,
    versionCode: Int = 0,
    versionName: String = "",
    channel: String = "",
) {
    /** Wird aufgerufen, wenn der Server die App-Version ablehnt (HTTP 426, Mindestversion im Admin-Bereich). */
    var onUpdateRequired: ((message: String) -> Unit)? = null

    private val prefs = context.getSharedPreferences("globeswap_api", Context.MODE_PRIVATE)
    private val tokenLock = Mutex()
    internal val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        // Version und Installationsquelle bei jeder Anfrage (auch Bilder und WebSocket) mitschicken.
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-App-Version-Code", versionCode.toString())
                    .header("X-App-Version-Name", versionName)
                    .header("X-App-Channel", channel)
                    .build()
            )
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    /** Für Coil: hängt das Token an jede Bildanfrage, Fotos sind nur für Beteiligte sichtbar. */
    val imageClient: OkHttpClient = http.newBuilder()
        .addInterceptor { chain ->
            val token = prefs.getString(KEY_TOKEN, null)
            val request = chain.request()
            chain.proceed(
                if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build()
            )
        }
        .build()

    val hasToken: Boolean get() = prefs.getString(KEY_TOKEN, null) != null

    fun photoUrl(photoId: String) = "$baseUrl/photos/$photoId"

    /** Adressen der rechtlichen Seiten auf dem eigenen Server (Datenschutz, Nutzungsbedingungen, …). */
    fun pageUrl(path: String) = "$baseUrl/$path"

    fun forgetIdentity() = prefs.edit().remove(KEY_TOKEN).remove(KEY_PUBLIC_ID).apply()

    /** Öffentliche UUID dieser Installation (für Anfragen per E-Mail, z. B. zur Datenlöschung). */
    suspend fun publicId(): String {
        prefs.getString(KEY_PUBLIC_ID, null)?.takeIf { it.isNotEmpty() }?.let { return it }
        val id = call("GET", "/api/me", MeResponse.serializer()).publicId
        prefs.edit().putString(KEY_PUBLIC_ID, id).apply()
        return id
    }

    private suspend fun token(): String = tokenLock.withLock {
        prefs.getString(KEY_TOKEN, null) ?: withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$baseUrl/api/register").post(EMPTY_BODY).build()
            val registered = http.newCall(request).execute().use { response ->
                if (response.code == HTTP_UPGRADE_REQUIRED) {
                    val text = response.body?.string().orEmpty()
                    onUpdateRequired?.invoke(errorField(text, "message").orEmpty())
                    throw ApiException(response.code, errorCode(text))
                }
                json.decodeFromString(RegisterResponse.serializer(), response.bodyOrThrow())
            }
            prefs.edit().putString(KEY_TOKEN, registered.token).putString(KEY_PUBLIC_ID, registered.publicId).apply()
            registered.token
        }
    }

    suspend fun <T> call(
        method: String,
        path: String,
        deserializer: DeserializationStrategy<T>,
        body: RequestBody? = null,
    ): T = json.decodeFromString(deserializer, callRaw(method, path, body))

    suspend fun callRaw(method: String, path: String, body: RequestBody? = null, retryAuth: Boolean = true): String {
        val token = token()
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer $token")
            .method(method, body ?: if (method == "POST") EMPTY_BODY else null)
            .build()
        val (status, text) = withContext(Dispatchers.IO) {
            http.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
        }
        if (status == 401 && retryAuth) {
            // Token ungültig (z. B. Server-Datenbank neu): neue anonyme Identität holen.
            tokenLock.withLock { if (prefs.getString(KEY_TOKEN, null) == token) forgetIdentity() }
            return callRaw(method, path, body, retryAuth = false)
        }
        if (status == HTTP_UPGRADE_REQUIRED) {
            onUpdateRequired?.invoke(errorField(text, "message").orEmpty())
        }
        if (status !in 200..299) throw ApiException(status, errorCode(text))
        return text
    }

    /** Mindestversion und neuestes Release – ohne Anmeldung, damit auch gesperrte Versionen es abfragen können. */
    suspend fun config(): UpdateConfig = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/config").build()
        http.newCall(request).execute().use { json.decodeFromString(UpdateConfig.serializer(), it.bodyOrThrow()) }
    }

    /** Hält eine WebSocket-Verbindung offen und verbindet sich bei Abbrüchen mit Backoff neu. */
    internal fun events(): Flow<ServerEvent> = channelFlow {
        val backoff = AtomicLong(1_000)
        while (isActive) {
            val token = runCatching { token() }.getOrNull()
            if (token != null) {
                val closed = CompletableDeferred<Unit>()
                val request = Request.Builder()
                    .url("$baseUrl/api/events")
                    .header("Authorization", "Bearer $token")
                    .build()
                val socket = http.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) = backoff.set(1_000)

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runCatching { json.decodeFromString(ServerEvent.serializer(), text) }
                            .onSuccess { trySend(it) }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(1000, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        closed.complete(Unit)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        closed.complete(Unit)
                    }
                })
                try {
                    closed.await()
                } finally {
                    socket.cancel()
                }
            }
            delay(backoff.get())
            backoff.set((backoff.get() * 2).coerceAtMost(60_000))
        }
    }

    private fun Response.bodyOrThrow(): String {
        val text = body?.string().orEmpty()
        if (!isSuccessful) throw ApiException(code, errorCode(text))
        return text
    }

    private fun errorCode(text: String): String = errorField(text, "error") ?: "unknown"

    private fun errorField(text: String, field: String): String? =
        runCatching { json.parseToJsonElement(text).jsonObject[field]?.jsonPrimitive?.content }.getOrNull()

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_PUBLIC_ID = "public_id"
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        val JPEG = "image/jpeg".toMediaType()
        val JSON = "application/json".toMediaType()
        const val HTTP_UPGRADE_REQUIRED = 426
    }
}
