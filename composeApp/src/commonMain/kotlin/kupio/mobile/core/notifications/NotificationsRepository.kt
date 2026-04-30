package kupio.mobile.core.notifications

interface NotificationsRepository {
    suspend fun sendPushToken(
        token: String,
    )
}