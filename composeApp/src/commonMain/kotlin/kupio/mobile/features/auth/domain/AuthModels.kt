package kupio.mobile.features.auth.domain

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long,
    val tokenType: String = "bearer",
    val needsUsername: Boolean = false,
)

data class AuthenticatedUser(
    val id: String,
    val username: String?,
    val displayName: String?,
    val email: String,
    val role: String,
    val needsUsername: Boolean,
    val balance: Int,
    val avatarUrl: String?,
)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class NeedsUsername(val user: AuthenticatedUser) : SessionState
    data class SignedIn(val user: AuthenticatedUser) : SessionState
}
