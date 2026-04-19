package kupio.mobile.features.auth.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.features.auth.domain.AuthRepository
import kupio.mobile.features.auth.domain.AuthSession
import kupio.mobile.features.auth.domain.AuthSessionManager
import kupio.mobile.features.auth.domain.AuthValidator
import kupio.mobile.features.auth.domain.AuthenticatedUser
import kupio.mobile.features.auth.domain.SessionState
import kupio.mobile.features.auth.domain.SessionStateResolver
import kupio.mobile.features.auth.domain.SecureSessionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
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
    fun `submit validates blank login form`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(AuthAction.SubmitClicked)

        assertEquals(kupio.mobile.features.auth.domain.FieldValidationError.Required, viewModel.state.value.emailError)
        assertEquals(kupio.mobile.features.auth.domain.FieldValidationError.Required, viewModel.state.value.passwordError)
    }

    @Test
    fun `mode switch enables register validation`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onAction(AuthAction.ModeSelected(AuthMode.REGISTER))
        viewModel.onAction(AuthAction.EmailChanged("hello@kupio.dev"))
        viewModel.onAction(AuthAction.PasswordChanged("password123"))
        viewModel.onAction(AuthAction.UsernameChanged("ab"))
        viewModel.onAction(AuthAction.SubmitClicked)

        assertEquals(kupio.mobile.features.auth.domain.FieldValidationError.UsernameTooShort, viewModel.state.value.usernameError)
    }

    @Test
    fun `successful register updates session manager state`() = runTest(dispatcher) {
        val sessionManager = AuthSessionManager(
            authRepository = FakeAuthRepository(),
            secureSessionStore = FakeSecureSessionStore(),
            sessionStateResolver = SessionStateResolver(),
        )
        val viewModel = createViewModel(
            repository = FakeAuthRepository(),
            sessionManager = sessionManager,
        )

        viewModel.onAction(AuthAction.ModeSelected(AuthMode.REGISTER))
        viewModel.onAction(AuthAction.EmailChanged("hello@kupio.dev"))
        viewModel.onAction(AuthAction.PasswordChanged("password123"))
        viewModel.onAction(AuthAction.UsernameChanged("kupio"))
        viewModel.onAction(AuthAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isSubmitting)
        assertEquals(
            SessionState.SignedIn(sampleUser()),
            sessionManager.sessionState.value,
        )
    }

    private fun createViewModel(
        repository: FakeAuthRepository = FakeAuthRepository(),
        sessionManager: AuthSessionManager = AuthSessionManager(
            authRepository = repository,
            secureSessionStore = FakeSecureSessionStore(),
            sessionStateResolver = SessionStateResolver(),
        ),
    ): AuthViewModel {
        return AuthViewModel(
            authRepository = repository,
            authValidator = AuthValidator(),
            sessionManager = sessionManager,
        )
    }

    private fun sampleSession() = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        accessExpiresAt = 100,
        refreshExpiresAt = 200,
    )

    private fun sampleUser() = AuthenticatedUser(
        id = "user-1",
        username = "kupio",
        displayName = "Kupio User",
        email = "hello@kupio.dev",
        role = "user",
        needsUsername = false,
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

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(email: String, password: String): AuthSession = SampleSession

        override suspend fun register(email: String, password: String, username: String): AuthSession = SampleSession

        override suspend fun loginWithGoogle(idToken: String): AuthSession = SampleSession

        override suspend fun refreshSession(): AuthSession = SampleSession

        override suspend fun getCurrentUser(): AuthenticatedUser = SampleUser

        override suspend fun setUsername(username: String): AuthenticatedUser = SampleUser.copy(username = username)

        override suspend fun clearSession() = Unit
    }

    private companion object {
        val SampleSession = AuthSession(
            accessToken = "access",
            refreshToken = "refresh",
            accessExpiresAt = 100,
            refreshExpiresAt = 200,
        )

        val SampleUser = AuthenticatedUser(
            id = "user-1",
            username = "kupio",
            displayName = "Kupio User",
            email = "hello@kupio.dev",
            role = "user",
            needsUsername = false,
            balance = 0,
            avatarUrl = null,
        )
    }
}
