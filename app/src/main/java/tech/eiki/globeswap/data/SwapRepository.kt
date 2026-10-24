package tech.eiki.globeswap.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Alle Aufrufe an den eigenen PhotoSwap-Server. Live-Updates kommen per WebSocket;
 * zusätzlich wird regelmäßig neu geladen, falls die Verbindung gerade weg ist.
 */
class SwapRepository(private val api: ApiClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = api.events().shareIn(scope, SharingStarted.WhileSubscribed(5_000))
    private val localChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    fun photoUrl(photoId: String) = api.photoUrl(photoId)

    fun pageUrl(path: String) = api.pageUrl(path)

    suspend fun publicId(): String = api.publicId()

    // ---------------------------------------------------------------- Swap

    suspend fun swap(jpeg: ByteArray): SwapResult {
        val response = api.call("POST", "/api/swap", SwapResponse.serializer(), jpeg.toRequestBody(ApiClient.JPEG))
        localChanges.tryEmit(Unit)
        val conversationId = response.conversationId
        return if (response.status == "matched" && conversationId != null) {
            SwapResult.Matched(conversationId)
        } else {
            SwapResult.Waiting(response.photoId)
        }
    }

    /** Liefert die Unterhaltungs-ID, sobald jemand das wartende Foto getauscht hat. */
    fun observeMatch(photoId: String): Flow<String?> =
        refreshes(10_000) { it.type == "hello" || (it.type == "matched" && it.photoId == photoId) }
            .transform {
                try {
                    val status = api.call("GET", "/api/swap/$photoId", SwapResponse.serializer())
                    emit(status.conversationId?.takeIf { status.status == "matched" })
                } catch (e: ApiException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Status nicht abrufbar", e)
                }
            }

    /** Zieht ein wartendes Foto zurück. false, wenn es inzwischen getauscht wurde. */
    suspend fun cancelWaiting(photoId: String): Boolean = try {
        api.callRaw("DELETE", "/api/swap/$photoId")
        true
    } catch (e: ApiException) {
        false
    }

    // ------------------------------------------------------- Unterhaltungen

    suspend fun fetchConversations(): List<Conversation> =
        api.call("GET", "/api/conversations", ListSerializer(Conversation.serializer()))

    fun observeConversations(): Flow<List<Conversation>> =
        refreshes(30_000) { true }.transform {
            runCatching { fetchConversations() }
                .onSuccess { emit(it) }
                .onFailure { Log.w(TAG, "Unterhaltungen nicht geladen", it) }
        }

    /** Emittiert null, wenn die Unterhaltung nicht (mehr) existiert. */
    fun observeConversation(conversationId: String): Flow<ConversationDetail?> =
        refreshes(30_000) { it.type == "hello" || it.type == "update" || it.conversationId == conversationId }
            .transform {
                try {
                    emit(api.call("GET", "/api/conversations/$conversationId", ConversationDetail.serializer()))
                } catch (e: ApiException) {
                    if (e.status == 404) emit(null) else Log.w(TAG, "Unterhaltung nicht geladen", e)
                } catch (e: Exception) {
                    Log.w(TAG, "Unterhaltung nicht geladen", e)
                }
            }

    suspend fun reply(conversationId: String, jpeg: ByteArray) {
        api.callRaw("POST", "/api/conversations/$conversationId/messages", jpeg.toRequestBody(ApiClient.JPEG))
        localChanges.tryEmit(Unit)
    }

    suspend fun markSeen(conversationId: String) {
        api.callRaw("POST", "/api/conversations/$conversationId/seen")
        localChanges.tryEmit(Unit)
    }

    /** Löscht die ganze Unterhaltung samt Fotos – auch bei der anderen Person. */
    suspend fun deleteConversation(conversationId: String) {
        api.callRaw("DELETE", "/api/conversations/$conversationId")
        localChanges.tryEmit(Unit)
    }

    /** Löscht ein eigenes Foto – auch bei der anderen Person. War es das letzte, verschwindet die Unterhaltung. */
    suspend fun deleteMessage(conversationId: String, messageId: String) {
        api.callRaw("DELETE", "/api/conversations/$conversationId/messages/$messageId")
        localChanges.tryEmit(Unit)
    }

    /** Meldet den Push-Schlüssel (Firebase) dieses Geräts beim Server an. */
    suspend fun registerPushToken(token: String) {
        val body = buildJsonObject { put("token", token) }
        api.callRaw("POST", "/api/push-token", body.toString().toRequestBody(ApiClient.JSON))
    }

    // ------------------------------------------------------- Moderation & Konto

    suspend fun report(conversationId: String, messageId: String?, reason: ReportReason) {
        val body = buildJsonObject {
            put("reason", reason.name)
            put("messageId", messageId)
        }
        api.callRaw("POST", "/api/conversations/$conversationId/report", body.toString().toRequestBody(ApiClient.JSON))
    }

    /** Blockiert die andere Person: Unterhaltung wird ausgeblendet, beide werden nie wieder gematcht. */
    suspend fun block(conversationId: String) {
        api.callRaw("POST", "/api/conversations/$conversationId/block")
        localChanges.tryEmit(Unit)
    }

    /** Löscht Konto, Unterhaltungen und Fotos auf dem Server; danach gibt es eine neue anonyme Identität. */
    suspend fun deleteAccount() {
        api.callRaw("DELETE", "/api/me", retryAuth = false)
        api.forgetIdentity()
        localChanges.tryEmit(Unit)
    }

    // ------------------------------------------------------- Hilfsfunktionen

    /** Sofort, bei passenden Server-Events, nach eigenen Änderungen und alle [periodMs]. */
    private fun refreshes(periodMs: Long, onEvent: (ServerEvent) -> Boolean): Flow<Unit> =
        merge(
            flowOf(Unit),
            events.filter(onEvent).map { },
            localChanges,
            flow {
                while (true) {
                    delay(periodMs)
                    emit(Unit)
                }
            },
        ).conflate()

    private companion object {
        const val TAG = "SwapRepository"
    }
}
