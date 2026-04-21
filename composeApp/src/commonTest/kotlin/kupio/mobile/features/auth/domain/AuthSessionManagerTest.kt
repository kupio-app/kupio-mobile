package kupio.mobile.features.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.auth.domain.session.SessionStateResolver

class AuthSessionManagerTest {
    @Test
    fun `bootstrap goes signed out when there is no stored session`() = runTest {
        val manager = createManager()

        manager.bootstrap()

        assertEquals(SessionState.SignedOut, manager.sessionState.value)
    }

    @Test
    fun `bootstrap clears session when refresh fails`() = runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = sampleSession(),
        )
        val manager = createManager(
            secureSessionStore = secureSessionStore,
            repository = FakeAuthRepository(
                refreshError = IllegalStateException("refresh failed"),
            ),
        )

        manager.bootstrap()

        assertEquals(SessionState.SignedOut, manager.sessionState.value)
        assertEquals(null, secureSessionStore.readSession())
    }

    @Test
    fun `bootstrap routes to username completion when backend requires it`() = runTest {
        val manager = createManager(
            secureSessionStore = FakeSecureSessionStore(session = sampleSession()),
            repository = FakeAuthRepository(
                refreshedSession = sampleSession(needsUsername = true),
                currentUser = sampleUser(needsUsername = true),
            ),
        )

        manager.bootstrap()

        assertEquals(
            SessionState.NeedsUsername(sampleUser(needsUsername = true)),
            manager.sessionState.value,
        )
    }

    @Test
    fun `establish session stores tokens and resolves signed in user`() = runTest {
        val secureSessionStore = FakeSecureSessionStore()
        val manager = createManager(
            secureSessionStore = secureSessionStore,
            repository = FakeAuthRepository(
                currentUser = sampleUser(needsUsername = false),
            ),
        )

        manager.establishSession(sampleSession())

        assertEquals(sampleSession(), secureSessionStore.readSession())
        assertEquals(
            SessionState.SignedIn(sampleUser(needsUsername = false)),
            manager.sessionState.value,
        )
    }

    @Test
    fun `sign out clears local session even when backend revoke fails`() = runTest {
        val secureSessionStore = FakeSecureSessionStore(session = sampleSession())
        val repository = FakeAuthRepository(
            logoutError = IllegalStateException("logout failed"),
        )
        val manager = createManager(
            repository = repository,
            secureSessionStore = secureSessionStore,
        )

        manager.signOut()

        assertEquals(1, repository.logoutCalls)
        assertEquals("refresh", repository.loggedOutRefreshToken)
        assertEquals(SessionState.SignedOut, manager.sessionState.value)
        assertEquals(null, secureSessionStore.readSession())
    }

    private fun createManager(
        repository: FakeAuthRepository = FakeAuthRepository(),
        secureSessionStore: FakeSecureSessionStore = FakeSecureSessionStore(),
    ): AuthSessionManager {
        return AuthSessionManager(
            authRepository = repository,
            secureSessionStore = secureSessionStore,
            sessionStateResolver = SessionStateResolver(),
        )
    }

    private fun sampleSession(
        needsUsername: Boolean = false,
    ) = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        accessExpiresAt = 100,
        refreshExpiresAt = 200,
        needsUsername = needsUsername,
    )

    private fun sampleUser(
        needsUsername: Boolean,
    ) = AuthenticatedUser(
        id = "user-1",
        username = if (needsUsername) null else "kupio_user",
        displayName = "Kupio User",
        email = "hello@kupio.dev",
        role = "user",
        needsUsername = needsUsername,
        balance = 0,
        avatarUrl = null,
    )

    private class FakeSecureSessionStore(
        private var session: AuthSession? = null,
    ) : SecureSessionStore {
        override suspend fun readSession(): AuthSession? = session

        override suspend fun writeSession(session: AuthSession) {
            this.session = session
        }

        override suspend fun clear() {
            session = null
        }
    }

    private class FakeAuthRepository(
        private val refreshedSession: AuthSession = AuthSession(
            accessToken = "fresh-access",
            refreshToken = "fresh-refresh",
            accessExpiresAt = 300,
            refreshExpiresAt = 400,
        ),
        private val currentUser: AuthenticatedUser = AuthenticatedUser(
            id = "user-1",
            username = "kupio_user",
            displayName = "Kupio User",
            email = "hello@kupio.dev",
            role = "user",
            needsUsername = false,
            balance = 0,
            avatarUrl = null,
        ),
        private val refreshError: Throwable? = null,
        private val logoutError: Throwable? = null,
    ) : AuthRepository {
        var logoutCalls: Int = 0
        var loggedOutRefreshToken: String? = null

        override suspend fun login(email: String, password: String): AuthSession = refreshedSession

        override suspend fun register(email: String, password: String, username: String): AuthSession = refreshedSession

        override suspend fun loginWithGoogle(idToken: String): AuthSession = refreshedSession

        override suspend fun refreshSession(): AuthSession {
            refreshError?.let { throw it }
            return refreshedSession
        }

        override suspend fun getCurrentUser(): AuthenticatedUser = currentUser

        override suspend fun setUsername(username: String): AuthenticatedUser = currentUser.copy(
            username = username,
            needsUsername = false,
        )

        override suspend fun logout(refreshToken: String) {
            logoutCalls += 1
            loggedOutRefreshToken = refreshToken
            logoutError?.let { throw it }
        }
    }
}
