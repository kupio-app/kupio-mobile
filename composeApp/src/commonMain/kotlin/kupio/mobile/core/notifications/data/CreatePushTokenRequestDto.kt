package kupio.mobile.core.notifications.data

import kotlinx.serialization.Serializable

@Serializable
data class CreatePushTokenRequestDto(
    val token: String,
    val platform: String
)