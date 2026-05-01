package kupio.mobile.core.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mmk.kmpnotifier.notification.NotifierManager
import kupio.mobile.R
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UnreadMessagesWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val chatsRepository: ChatsRepository by inject()

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        return try {
            val unreadCount = chatsRepository.getUnreadCount()
            val notificationShown = unreadCount > 0

            if (notificationShown) {
                val res = applicationContext.resources
                NotifierManager.getLocalNotifier().notify(
                    id = NOTIFICATION_ID,
                    title = res.getString(R.string.notif_unread_title),
                    body = res.getQuantityString(R.plurals.notif_unread_body, unreadCount, unreadCount),
                )
            }

            val metrics = SyncMetrics(
                timestampMs = startTime,
                durationMs = System.currentTimeMillis() - startTime,
                unreadCount = unreadCount,
                notificationShown = notificationShown,
                success = true,
            )
            Log.i(LOG_TAG, "Sync completed: $metrics")

            Result.success(
                workDataOf(
                    KEY_TIMESTAMP_MS to metrics.timestampMs,
                    KEY_DURATION_MS to metrics.durationMs,
                    KEY_UNREAD_COUNT to metrics.unreadCount,
                    KEY_NOTIFICATION_SHOWN to metrics.notificationShown,
                )
            )
        } catch (_: AuthSessionExpiredException) {
            Log.w(LOG_TAG, "Sync skipped: session expired")
            Result.failure()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Sync failed, will retry: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "kupio.unread_messages_sync"
        private const val NOTIFICATION_ID = 1001
        private const val LOG_TAG = "UnreadMessagesWorker"
        const val KEY_TIMESTAMP_MS = "timestamp_ms"
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_UNREAD_COUNT = "unread_count"
        const val KEY_NOTIFICATION_SHOWN = "notification_shown"
    }
}
