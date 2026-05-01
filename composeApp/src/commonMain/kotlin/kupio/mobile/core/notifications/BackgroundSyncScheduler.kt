package kupio.mobile.core.notifications

interface BackgroundSyncScheduler {
    fun schedule()
    fun cancel()
}
