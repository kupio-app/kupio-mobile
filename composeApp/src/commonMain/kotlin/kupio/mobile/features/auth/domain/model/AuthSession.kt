package kupio.mobile.features.auth.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long,
    val tokenType: String = "bearer",
    val needsUsername: Boolean = false,
)
