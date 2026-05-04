package kupio.mobile.core.offline

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val offlineSyncManager: OfflineSyncManager by inject()

    override suspend fun doWork(): Result =
        try {
            offlineSyncManager.syncPending()
            Result.success()
        } catch (_: AuthSessionExpiredException) {
            Log.w(LOG_TAG, "Offline sync skipped: session expired")
            Result.failure()
        } catch (throwable: Throwable) {
            Log.w(LOG_TAG, "Offline sync failed, will retry: ${throwable.message}")
            Result.retry()
        }

    companion object {
        const val WORK_NAME = "kupio.offline_sync"
        private const val LOG_TAG = "OfflineSyncWorker"
    }
}
