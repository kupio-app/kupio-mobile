package kupio.mobile.features.auth.domain

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): AuthSession

    suspend fun register(
        email: String,
        password: String,
        username: String,
    ): AuthSession

    suspend fun loginWithGoogle(
        idToken: String,
    ): AuthSession

    suspend fun refreshSession(): AuthSession

    suspend fun getCurrentUser(): AuthenticatedUser

    suspend fun setUsername(
        username: String,
    ): AuthenticatedUser

    suspend fun logout(
        refreshToken: String,
    )

    suspend fun clearSession()
}
