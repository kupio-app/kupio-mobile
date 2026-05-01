package kupio.mobile.core.notifications

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BackgroundSyncSchedulerImpl(
    private val workManager: WorkManager,
) : BackgroundSyncScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<UnreadMessagesWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            UnreadMessagesWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(UnreadMessagesWorker.WORK_NAME)
    }
}
