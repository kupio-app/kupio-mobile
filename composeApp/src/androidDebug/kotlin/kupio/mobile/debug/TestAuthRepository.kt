package kupio.mobile.debug

import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.CachedAuthenticatedUserStore
import kupio.mobile.features.auth.domain.session.SecureSessionStore

internal class TestAuthRepository(
    private val delegate: AuthRepository,
    private val secureSessionStore: SecureSessionStore,
    private val cachedAuthenticatedUserStore: CachedAuthenticatedUserStore,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthSession = delegate.login(email, password)

    override suspend fun register(
        email: String,
        password: String,
        username: String,
    ): AuthSession = delegate.register(email, password, username)

    override suspend fun loginWithGoogle(
        idToken: String,
    ): AuthSession = delegate.loginWithGoogle(idToken)

    override suspend fun refreshSession(): AuthSession {
        return seededSessionOrNull() ?: delegate.refreshSession()
    }

    override suspend fun getCurrentUser(): AuthenticatedUser {
        return seededUserOrNull() ?: delegate.getCurrentUser()
    }

    override suspend fun setUsername(
        username: String,
    ): AuthenticatedUser = delegate.setUsername(username)

    override suspend fun logout(
        refreshToken: String,
    ) = delegate.logout(refreshToken)

    private suspend fun seededSessionOrNull(): AuthSession? {
        val session = secureSessionStore.readSession() ?: return null
        val cachedUser = cachedAuthenticatedUserStore.read() ?: return null
        return if (
            session.accessToken == TestAuthContract.AccessToken &&
            session.refreshToken == TestAuthContract.RefreshToken &&
            cachedUser.id.isNotBlank()
        ) {
            session
        } else {
            null
        }
    }

    private suspend fun seededUserOrNull(): AuthenticatedUser? {
        val session = secureSessionStore.readSession() ?: return null
        val cachedUser = cachedAuthenticatedUserStore.read() ?: return null
        return if (
            session.accessToken == TestAuthContract.AccessToken &&
            session.refreshToken == TestAuthContract.RefreshToken
        ) {
            cachedUser
        } else {
            null
        }
    }
}
