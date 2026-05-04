package kupio.mobile.core.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
class IosOfflineSyncScheduler(
    private val offlineSyncManager: OfflineSyncManager,
    private val appScope: CoroutineScope,
) : OfflineSyncScheduler {
    private val monitor = nw_path_monitor_create()
    private var started = false

    override fun start() {
        if (started) return
        started = true
        nw_path_monitor_set_update_handler(monitor) { path ->
            if (nw_path_get_status(path) == nw_path_status_satisfied) {
                requestSync()
            }
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        requestSync()
    }

    override fun requestSync() {
        appScope.launch {
            runCatching { offlineSyncManager.syncPending() }
        }
    }

    override fun cancel() {
        nw_path_monitor_cancel(monitor)
        started = false
    }
}
