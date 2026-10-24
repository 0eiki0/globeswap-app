package tech.eiki.globeswap.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Schaut etwa alle 15 Minuten beim eigenen Server nach neuen Fotos – ohne Google-Dienste und als
 * Reserve, falls eine Push-Mitteilung nicht ankommt.
 */
class NewPhotoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        if (NewPhotoNotifier.check(applicationContext)) Result.success() else Result.retry()

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewPhotoWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("new-photos", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
