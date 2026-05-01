package kupio.mobile.core.notifications

import com.mmk.kmpnotifier.notification.NotifierManager
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun performUnreadSync(
    title: String,
    body: (Int) -> String,
    onComplete: (Boolean) -> Unit,
) {
    val startTime = currentEpochMillis()
    syncScope.launch {
        try {
            val koin = KoinPlatformTools.defaultContext().get()
            val unreadCount = koin.get<ChatsRepository>().getUnreadCount()
            val notificationShown = unreadCount > 0

            if (notificationShown) {
                NotifierManager.getLocalNotifier().notify(
                    id = NOTIFICATION_ID,
                    title = title,
                    body = body(unreadCount),
                )
            }

            val metrics = SyncMetrics(
                timestampMs = startTime,
                durationMs = currentEpochMillis() - startTime,
                unreadCount = unreadCount,
                notificationShown = notificationShown,
                success = true,
            )
            println("[UnreadSync] Sync completed: $metrics")

            koin.get<BackgroundSyncScheduler>().schedule()
            onComplete(true)
        } catch (e: Exception) {
            println("[UnreadSync] Sync failed: ${e.message}")
            onComplete(false)
        }
    }
}

private fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

private const val NOTIFICATION_ID = 1001
