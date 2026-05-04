package kupio.mobile.features.auth.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.network.ApiException
import kupio.mobile.core.network.ApiFieldError
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.FieldValidationError
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.model.UserRole
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.CachedAuthenticatedUserStore
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.auth.domain.validation.AuthValidator
import kupio.mobile.features.auth.presentation.auth.AuthEffect
import kupio.mobile.features.auth.presentation.auth.AuthFormError
import kupio.mobile.features.auth.presentation.auth.AuthIntent
import kupio.mobile.features.auth.presentation.auth.AuthMode
import kupio.mobile.features.auth.presentation.auth.AuthState
import kupio.mobile.features.auth.presentation.auth.AuthViewModel
import kupio.mobile.features.auth.presentation.auth.GoogleSignInFailureCode

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

        viewModel.onIntent(AuthIntent.SubmitClicked)

        assertEquals(FieldValidationError.Required, viewModel.state.value.emailError)
        assertEquals(FieldValidationError.Required, viewModel.state.value.passwordError)
    }

    @Test
    fun `mode switch enables register validation`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.REGISTER))
        viewModel.onIntent(AuthIntent.EmailChanged("hello@kupio.dev"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.UsernameChanged("ab"))
        viewModel.onIntent(AuthIntent.SubmitClicked)

        assertEquals(FieldValidationError.UsernameTooShort, viewModel.state.value.usernameError)
    }

    @Test
    fun `register validates confirm password before submit`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = createViewModel(repository = repository)

        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.REGISTER))
        viewModel.onIntent(AuthIntent.EmailChanged("hello@kupio.dev"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password124"))
        viewModel.onIntent(AuthIntent.UsernameChanged("kupio"))
        viewModel.onIntent(AuthIntent.SubmitClicked)
        advanceUntilIdle()

        assertEquals(FieldValidationError.PasswordsDoNotMatch, viewModel.state.value.confirmPasswordError)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `mode switch to login clears register only fields`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.REGISTER))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.UsernameChanged("kupio"))
        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.LOGIN))

        assertEquals("", viewModel.state.value.confirmPassword)
        assertEquals("", viewModel.state.value.username)
    }

    @Test
    fun `fresh view model starts with empty auth state`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        assertEquals(AuthState(), viewModel.state.value)
    }

    @Test
    fun `google click emits launch effect and toggles loading state`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(AuthIntent.GoogleClicked)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isGoogleSubmitting)
        assertEquals(AuthEffect.LaunchGoogleSignIn, viewModel.effects.first())
    }

    @Test
    fun `google failure resets loading and exposes message`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(AuthIntent.GoogleClicked)
        viewModel.onIntent(AuthIntent.GoogleFailure(GoogleSignInFailureCode.Failed))

        assertEquals(false, viewModel.state.value.isGoogleSubmitting)
        assertEquals(
            AuthFormError.GoogleSignIn(GoogleSignInFailureCode.Failed),
            viewModel.state.value.formError,
        )
    }

    @Test
    fun `api field errors are mapped into form field state`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            repository = FakeAuthRepository(
                registerError = ApiException(
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
            ),
        )

        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.REGISTER))
        viewModel.onIntent(AuthIntent.EmailChanged("hello@kupio.dev"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.UsernameChanged("ab"))
        viewModel.onIntent(AuthIntent.SubmitClicked)
        advanceUntilIdle()

        assertEquals(FieldValidationError.UsernameTooShort, viewModel.state.value.usernameError)
        assertEquals(null, viewModel.state.value.formError)
    }

    @Test
    fun `successful register updates session manager state`() = runTest(dispatcher) {
        val sessionManager = AuthSessionManager(
            authRepository = FakeAuthRepository(),
            secureSessionStore = FakeSecureSessionStore(),
            cachedAuthenticatedUserStore = FakeCachedAuthenticatedUserStore(),
        )
        val viewModel = createViewModel(
            repository = FakeAuthRepository(),
            sessionManager = sessionManager,
        )

        viewModel.onIntent(AuthIntent.ModeSelected(AuthMode.REGISTER))
        viewModel.onIntent(AuthIntent.EmailChanged("hello@kupio.dev"))
        viewModel.onIntent(AuthIntent.PasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.ConfirmPasswordChanged("password123"))
        viewModel.onIntent(AuthIntent.UsernameChanged("kupio"))
        viewModel.onIntent(AuthIntent.SubmitClicked)
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
            cachedAuthenticatedUserStore = FakeCachedAuthenticatedUserStore(),
        ),
    ): AuthViewModel {
        return AuthViewModel(
            authRepository = repository,
            authValidator = AuthValidator(),
            sessionManager = sessionManager,
        )
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

    private class FakeCachedAuthenticatedUserStore : CachedAuthenticatedUserStore {
        override suspend fun read(): AuthenticatedUser? = null
        override suspend fun write(user: AuthenticatedUser) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeAuthRepository(
        private val registerError: Throwable? = null,
    ) : AuthRepository {
        var registerCalls: Int = 0

        override suspend fun login(email: String, password: String): AuthSession = SampleSession

        override suspend fun register(email: String, password: String, username: String): AuthSession {
            registerCalls += 1
            registerError?.let { throw it }
            return SampleSession
        }

        override suspend fun loginWithGoogle(idToken: String): AuthSession = SampleSession

        override suspend fun refreshSession(): AuthSession = SampleSession

        override suspend fun getCurrentUser(): AuthenticatedUser = SampleUser

        override suspend fun setUsername(username: String): AuthenticatedUser = SampleUser.copy(username = username)

        override suspend fun logout(refreshToken: String) = Unit
    }

    private companion object {
        val SampleSession = AuthSession(
            accessToken = "access",
            refreshToken = "refresh",
            accessExpiresAt = 100,
            refreshExpiresAt = 200,
        )

        val SampleUser = sampleUser()
    }
}

private fun sampleUser() = AuthenticatedUser(
    id = "user-1",
    username = "kupio",
    displayName = "Kupio User",
    email = "hello@kupio.dev",
    role = UserRole.USER,
    needsUsername = false,
    balance = 0,
    avatarUrl = null,
)
