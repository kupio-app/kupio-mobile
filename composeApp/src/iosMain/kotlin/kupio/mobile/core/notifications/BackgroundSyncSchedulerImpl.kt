package kupio.mobile.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate

class BackgroundSyncSchedulerImpl : BackgroundSyncScheduler {

    @OptIn(ExperimentalForeignApi::class)
    override fun schedule() {
        val request = BGAppRefreshTaskRequest(TASK_IDENTIFIER).apply {
            earliestBeginDate = NSDate(REPEAT_INTERVAL_SECONDS)
        }
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
    }

    override fun cancel() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    }

    companion object {
        const val TASK_IDENTIFIER = "kupio.mobile.unread_messages_sync"
        private const val REPEAT_INTERVAL_SECONDS = 15.0 * 60
    }
}
