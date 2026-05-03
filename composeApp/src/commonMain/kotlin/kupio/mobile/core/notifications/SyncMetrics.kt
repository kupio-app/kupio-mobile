package kupio.mobile.core.notifications

data class SyncMetrics(
    val timestampMs: Long,
    val durationMs: Long,
    val unreadCount: Int,
    val notificationShown: Boolean,
    val success: Boolean,
)
