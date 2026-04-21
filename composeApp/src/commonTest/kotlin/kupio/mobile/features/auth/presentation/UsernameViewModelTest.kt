package kupio.mobile.features.auth.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.network.ApiException
import kupio.mobile.core.network.ApiFieldError
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.FieldValidationError
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.auth.domain.validation.AuthValidator
import kupio.mobile.features.auth.presentation.username.UsernameIntent
import kupio.mobile.features.auth.presentation.username.UsernameViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameViewModelTest {
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
    fun `submit validates blank username`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(UsernameIntent.SubmitClicked)

        assertEquals(FieldValidationError.Required, viewModel.state.value.usernameError)
    }

    @Test
    fun `successful submit updates auth session state`() = runTest(dispatcher) {
        val sessionManager = createSessionManager()
        val viewModel = createViewModel(sessionManager = sessionManager)

        viewModel.onIntent(UsernameIntent.UsernameChanged("kupio"))
        viewModel.onIntent(UsernameIntent.SubmitClicked)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isSubmitting)
        assertEquals(
            SessionState.SignedIn(sampleUsernameUser(needsUsername = false)),
            sessionManager.sessionState.value,
        )
    }

    @Test
    fun `api username error is mapped into field state`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            usernameError = ApiException(
                statusCode = 422,
                message = "Validation failed",
                fieldErrors = listOf(
                    ApiFieldError(
                        field = "username",
                        message = "Any backend text",
                        type = "string_too_short",
                        context = mapOf("min_length" to "3"),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel(repository = repository)

        viewModel.onIntent(UsernameIntent.UsernameChanged("valid-enough"))
        viewModel.onIntent(UsernameIntent.SubmitClicked)
        advanceUntilIdle()

        assertEquals(FieldValidationError.UsernameTooShort, viewModel.state.value.usernameError)
        assertEquals(null, viewModel.state.value.formError)
    }

    @Test
    fun `expired session during submit signs out`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            usernameError = AuthSessionExpiredException(),
        )
        val sessionManager = createSessionManager(repository = repository)
        val viewModel = createViewModel(
            repository = repository,
            sessionManager = sessionManager,
        )

        viewModel.onIntent(UsernameIntent.UsernameChanged("kupio"))
        viewModel.onIntent(UsernameIntent.SubmitClicked)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isSubmitting)
        assertEquals(SessionState.SignedOut, sessionManager.sessionState.value)
    }

    private suspend fun createViewModel(
        repository: FakeAuthRepository = FakeAuthRepository(),
        sessionManager: AuthSessionManager? = null,
    ): UsernameViewModel {
        return UsernameViewModel(
            authRepository = repository,
            authValidator = AuthValidator(),
            sessionManager = sessionManager ?: createSessionManager(repository),
        )
    }

    private suspend fun createSessionManager(
        repository: AuthRepository = FakeAuthRepository(),
    ): AuthSessionManager {
        val manager = AuthSessionManager(
            authRepository = repository,
            secureSessionStore = FakeSecureSessionStore(
                session = AuthSession(
                    accessToken = "access",
                    refreshToken = "refresh",
                    accessExpiresAt = 100,
                    refreshExpiresAt = 200,
                ),
            ),
        )
        manager.updateAuthenticatedUser(sampleUsernameUser(needsUsername = true))
        return manager
    }

    private class FakeAuthRepository(
        private val usernameError: Throwable? = null,
    ) : AuthRepository {
        override suspend fun login(email: String, password: String): AuthSession = error("Unused")
        override suspend fun register(email: String, password: String, username: String): AuthSession = error("Unused")
        override suspend fun loginWithGoogle(idToken: String): AuthSession = error("Unused")
        override suspend fun refreshSession(): AuthSession = error("Unused")
        override suspend fun getCurrentUser(): AuthenticatedUser = sampleUsernameUser(needsUsername = true)

        override suspend fun setUsername(username: String): AuthenticatedUser {
            usernameError?.let { throw it }
            return sampleUsernameUser(needsUsername = false).copy(username = username)
        }

        override suspend fun logout(refreshToken: String) = Unit
    }

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
}

private fun sampleUsernameUser(
    needsUsername: Boolean,
) = AuthenticatedUser(
    id = "user-1",
    username = if (needsUsername) null else "kupio",
    displayName = "Kupio User",
    email = "hello@kupio.dev",
    role = "user",
    needsUsername = needsUsername,
    balance = 0,
    avatarUrl = null,
)
