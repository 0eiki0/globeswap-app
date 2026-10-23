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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import tech.eiki.globeswap.MainActivity
import tech.eiki.globeswap.R
import tech.eiki.globeswap.data.ServiceLocator
import java.util.concurrent.TimeUnit

/**
 * Schaut ohne Google-Dienste etwa alle 15 Minuten beim eigenen Server nach neuen Fotos
 * und zeigt eine Benachrichtigung, wenn die App gerade nicht offen ist.
 */
class NewPhotoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)
        if (!ServiceLocator.api.hasToken) return Result.success()

        val conversations = runCatching { ServiceLocator.repository.fetchConversations() }
            .getOrElse { return Result.retry() }
        val unread = conversations.filter { it.unread }
        val newest = unread.maxOfOrNull { it.lastMessageAt } ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (newest <= prefs.getLong(KEY_LAST_NOTIFIED, 0) || appInForeground()) return Result.success()
        prefs.edit().putLong(KEY_LAST_NOTIFIED, newest).apply()
        notify(unread.size)
        return Result.success()
    }

    private fun appInForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun notify(count: Int) {
        val context = applicationContext
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

    companion object {
        private const val CHANNEL_ID = "new_photos"
        private const val NOTIFICATION_ID = 1
        private const val PREFS = "globeswap_notifications"
        private const val KEY_LAST_NOTIFIED = "last_notified_at"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "New photos", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewPhotoWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("new-photos", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
