package kupio.mobile.core.offline

interface OfflineSyncScheduler {
    fun start()
    fun requestSync()
    fun cancel()
}
