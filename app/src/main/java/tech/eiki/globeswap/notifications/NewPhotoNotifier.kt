package tech.eiki.globeswap.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import tech.eiki.globeswap.MainActivity
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.ServiceLocator

/**
 * Fragt beim Server nach ungelesenen Fotos und zeigt eine Benachrichtigung, wenn die App nicht offen ist.
 * Wird von der 15-Minuten-Abfrage und von Push-Mitteilungen benutzt; dasselbe Foto meldet es nur einmal.
 */
object NewPhotoNotifier {
    private const val CHANNEL_ID = "new_photos"
    private const val NOTIFICATION_ID = 1
    private const val PREFS = "globeswap_notifications"
    private const val KEY_LAST_NOTIFIED = "last_notified_at"

    /** false, wenn der Server nicht erreichbar war (später erneut versuchen). */
    suspend fun check(context: Context): Boolean {
        ServiceLocator.init(context)
        if (!ServiceLocator.api.hasToken) return true

        val conversations = runCatching { ServiceLocator.repository.fetchConversations() }
            .getOrElse { return false }
        val unread = conversations.filter { it.unread }
        val newest = unread.maxOfOrNull { it.lastMessageAt } ?: return true

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        synchronized(this) {
            if (newest <= prefs.getLong(KEY_LAST_NOTIFIED, 0) || appInForeground()) return true
            prefs.edit().putLong(KEY_LAST_NOTIFIED, newest).apply()
        }
        notify(context, unread.size)
        return true
    }

    /**
     * Nach dem Lesen: Ist nichts Ungelesenes mehr da, verschwindet die Benachrichtigung – und mit ihr der
     * Punkt am App-Symbol (er gehört zur Benachrichtigung). Sonst würde sie nur beim Antippen verschwinden.
     */
    suspend fun clearIfAllRead(context: Context) {
        val conversations = runCatching { ServiceLocator.repository.fetchConversations() }.getOrNull() ?: return
        if (conversations.none { it.unread }) cancel(context)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "New photos", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun appInForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun notify(context: Context, count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (count == 1) "New photo for you" else "$count new photos for you")
            .setContentText("Someone somewhere in the world sent you a photo.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
