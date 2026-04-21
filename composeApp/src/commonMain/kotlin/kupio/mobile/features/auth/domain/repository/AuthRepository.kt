package kupio.mobile.features.auth.domain.repository

import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser

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
}
