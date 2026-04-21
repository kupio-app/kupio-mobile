package kupio.mobile.features.auth.domain.model

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
