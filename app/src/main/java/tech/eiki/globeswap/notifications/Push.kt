package tech.eiki.globeswap.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import tech.eiki.globeswap.BuildConfig
import tech.eiki.globeswap.data.ServiceLocator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sofortige Benachrichtigungen über Firebase Cloud Messaging. Die App meldet sich nur bei Google an,
 * wenn sie mit Firebase gebaut wurde und der eigene Server Push auch verschickt (/api/config).
 * Ohne Google-Dienste oder ohne Push bleibt es bei der Abfrage alle 15 Minuten.
 */
object Push {
    private const val TAG = "Push"

    val configured: Boolean get() = BuildConfig.FIREBASE_APP_ID.isNotEmpty()

    /** Richtet Firebase ein (ohne Netzwerkzugriff); muss vor jeder Push-Nachricht passieren. */
    fun init(context: Context) {
        if (!configured || FirebaseApp.getApps(context).isNotEmpty()) return
        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        runCatching { FirebaseApp.initializeApp(context, options) }
            .onFailure { Log.w(TAG, "Firebase nicht verfügbar", it) }
    }

    /** Holt den Push-Schlüssel und meldet ihn beim Server an. Erst nach der Anmeldung beim Server. */
    fun register(context: Context) {
        if (!configured) return
        ServiceLocator.init(context)
        if (!ServiceLocator.api.hasToken) return
        ServiceLocator.appScope.launch {
            try {
                if (!ServiceLocator.api.config().push) return@launch
                init(context)
                sendToken(fetchToken())
            } catch (e: Exception) {
                // z. B. Gerät ohne Google-Dienste: dann bleibt es bei der 15-Minuten-Abfrage.
                Log.w(TAG, "Push nicht verfügbar", e)
            }
        }
    }

    internal suspend fun sendToken(token: String) {
        ServiceLocator.repository.registerPushToken(token)
    }

    private suspend fun fetchToken(): String = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) cont.resume(task.result) else cont.resumeWithException(task.exception ?: IllegalStateException())
        }
    }
}

/** Empfängt Push-Mitteilungen ("neues Foto") und neue Push-Schlüssel von Firebase. */
class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        ServiceLocator.init(applicationContext)
        if (!ServiceLocator.api.hasToken) return
        ServiceLocator.appScope.launch {
            runCatching { Push.sendToken(token) }.onFailure { Log.w("Push", "Schlüssel nicht gesendet", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Der Push enthält nur "neues Foto in Unterhaltung X"; was angezeigt wird, holt die App selbst.
        runBlocking { withTimeoutOrNull(15_000) { NewPhotoNotifier.check(applicationContext) } }
    }
}
