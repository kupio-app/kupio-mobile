package kupio.mobile.core.offline

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidOfflineSyncScheduler(
    private val workManager: WorkManager,
) : OfflineSyncScheduler {

    override fun start() {
        requestSync()
    }

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            OfflineSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(OfflineSyncWorker.WORK_NAME)
    }

    private companion object {
        const val RETRY_BACKOFF_DELAY_SECONDS = 30L
    }
}
