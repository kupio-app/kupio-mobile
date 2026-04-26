package kupio.mobile.features.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout action signs out current session`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository()
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                accessExpiresAt = 1,
                refreshExpiresAt = 2,
            ),
        )
        val sessionManager = AuthSessionManager(
            authRepository = authRepository,
            secureSessionStore = secureSessionStore,
        )
        val viewModel = SettingsViewModel(
            preferencesRepository = FakePreferencesRepository(),
            sessionManager = sessionManager,
        )

        viewModel.onAction(SettingsAction.LogoutClicked)
        advanceUntilIdle()

        assertEquals(1, authRepository.logoutCalls)
        assertEquals(SessionState.SignedOut, sessionManager.sessionState.value)
        assertEquals(false, viewModel.state.value.isSigningOut)
        assertEquals(null, secureSessionStore.readSession())
    }

    @Test
    fun `logout action resets loading state when sign out fails`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            preferencesRepository = FakePreferencesRepository(),
            sessionManager = AuthSessionManager(
                authRepository = FakeAuthRepository(),
                secureSessionStore = FakeSecureSessionStore(
                    session = AuthSession(
                        accessToken = "access",
                        refreshToken = "refresh",
                        accessExpiresAt = 1,
                        refreshExpiresAt = 2,
                    ),
                    clearError = IllegalStateException("clear failed"),
                ),
            ),
        )

        viewModel.onAction(SettingsAction.LogoutClicked)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isSigningOut)
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

        override suspend fun setThemeMode(mode: ThemeMode) {
            themeMode.value = mode
        }

        override fun chatLastSeenEpochMillis(conversationId: String) = MutableStateFlow<Long?>(null)

        override suspend fun markChatSeen(conversationId: String, epochMillis: Long) = Unit
    }

    private class FakeSecureSessionStore(
        private var session: AuthSession? = null,
        private val clearError: Throwable? = null,
    ) : SecureSessionStore {
        override suspend fun readSession(): AuthSession? = session

        override suspend fun writeSession(session: AuthSession) {
            this.session = session
        }

        override suspend fun clear() {
            clearError?.let { throw it }
            session = null
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutCalls: Int = 0

        override suspend fun login(email: String, password: String): AuthSession = error("Unused")

        override suspend fun register(email: String, password: String, username: String): AuthSession = error("Unused")

        override suspend fun loginWithGoogle(idToken: String): AuthSession = error("Unused")

        override suspend fun refreshSession(): AuthSession = error("Unused")

        override suspend fun getCurrentUser(): AuthenticatedUser = error("Unused")

        override suspend fun setUsername(username: String): AuthenticatedUser = error("Unused")

        override suspend fun logout(refreshToken: String) {
            logoutCalls += 1
        }
    }
}
