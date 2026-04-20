package kupio.mobile.features.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.auth.domain.AuthSession
import kupio.mobile.features.auth.domain.AuthenticatedUser

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val username: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class GoogleLoginRequestDto(
    @SerialName("id_token") val idToken: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SetUsernameRequestDto(
    val username: String,
)

@Serializable
data class TokensResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("access_expires_at") val accessExpiresAt: Long,
    @SerialName("refresh_expires_at") val refreshExpiresAt: Long,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("needs_username") val needsUsername: Boolean = false,
)

@Serializable
data class UserPrivateDto(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val email: String,
    val role: String,
    @SerialName("needs_username") val needsUsername: Boolean,
    val balance: Int,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

fun TokensResponseDto.toDomain(): AuthSession {
    return AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessExpiresAt = accessExpiresAt,
        refreshExpiresAt = refreshExpiresAt,
        tokenType = tokenType,
        needsUsername = needsUsername,
    )
}

fun UserPrivateDto.toDomain(): AuthenticatedUser {
    return AuthenticatedUser(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
        role = role,
        needsUsername = needsUsername,
        balance = balance,
        avatarUrl = avatarUrl,
    )
}
