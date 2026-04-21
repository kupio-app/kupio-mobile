package kupio.mobile.features.auth.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.validation.AuthValidator

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val authValidator: AuthValidator,
    private val sessionManager: AuthSessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = effectChannel.receiveAsFlow()

    fun onIntent(
        intent: AuthIntent,
    ) {
        when (intent) {
            is AuthIntent.ModeSelected -> updateMode(intent.mode)
            is AuthIntent.EmailChanged -> updateEmail(intent.value)
            is AuthIntent.PasswordChanged -> updatePassword(intent.value)
            is AuthIntent.ConfirmPasswordChanged -> updateConfirmPassword(intent.value)
            is AuthIntent.UsernameChanged -> updateUsername(intent.value)
            AuthIntent.GoogleCancelled -> resetGoogleSubmission()
            AuthIntent.GoogleClicked -> requestGoogleSignIn()
            is AuthIntent.GoogleFailure -> setGoogleFailure(intent.message)
            is AuthIntent.GoogleSuccess -> submitGoogleIdToken(intent.idToken)
            AuthIntent.SubmitClicked -> submit()
        }
    }

    private fun resetForm() {
        if (_state.value == AuthState()) return
        _state.value = AuthState()
    }

    private fun updateMode(
        mode: AuthMode,
    ) {
        if (_state.value.mode == mode) return

        _state.update {
            it.copy(
                mode = mode,
                confirmPassword = if (mode == AuthMode.LOGIN) "" else it.confirmPassword,
                username = if (mode == AuthMode.LOGIN) "" else it.username,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                usernameError = null,
                formError = null,
                isSubmitting = false,
                isGoogleSubmitting = false,
            )
        }
    }

    private fun updateEmail(
        value: String,
    ) {
        if (_state.value.email == value) return

        _state.update {
            it.copy(
                email = value,
                emailError = null,
                formError = null,
            )
        }
    }

    private fun updatePassword(
        value: String,
    ) {
        if (_state.value.password == value) return

        _state.update {
            it.copy(
                password = value,
                passwordError = null,
                confirmPasswordError = null,
                formError = null,
            )
        }
    }

    private fun updateConfirmPassword(
        value: String,
    ) {
        if (_state.value.confirmPassword == value) return

        _state.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = null,
                formError = null,
            )
        }
    }

    private fun updateUsername(
        value: String,
    ) {
        if (_state.value.username == value) return

        _state.update {
            it.copy(
                username = value,
                usernameError = null,
                formError = null,
            )
        }
    }

    private fun submit() {
        val currentState = _state.value
        if (currentState.isBusy) return

        val emailError = authValidator.validateEmail(currentState.email)
        val passwordError = authValidator.validatePassword(currentState.password)
        val confirmPasswordError = if (currentState.mode == AuthMode.REGISTER) {
            authValidator.validateConfirmPassword(
                password = currentState.password,
                confirmPassword = currentState.confirmPassword,
            )
        } else {
            null
        }
        val usernameError = if (currentState.mode == AuthMode.REGISTER) {
            authValidator.validateUsername(currentState.username)
        } else {
            null
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null || usernameError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    usernameError = usernameError,
                    formError = null,
                )
            }
            return
        }

        val email = currentState.email.trim()
        val password = currentState.password
        val username = currentState.username.trim()
        val mode = currentState.mode

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    formError = null,
                )
            }

            runCatching {
                when (mode) {
                    AuthMode.LOGIN -> authRepository.login(
                        email = email,
                        password = password,
                    )

                    AuthMode.REGISTER -> authRepository.register(
                        email = email,
                        password = password,
                        username = username,
                    )
                }
            }.onSuccess { session ->
                completeSession(
                    session = session,
                    onFailure = { message ->
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                formError = message,
                            )
                        }
                    },
                    onComplete = {
                        _state.update { it.copy(isSubmitting = false) }
                    },
                )
            }.onFailure { throwable ->
                applyApiError(throwable)
            }
        }
    }

    private fun requestGoogleSignIn() {
        val currentState = _state.value
        if (currentState.isBusy) return

        _state.update {
            it.copy(
                formError = null,
                isGoogleSubmitting = true,
            )
        }

        viewModelScope.launch {
            effectChannel.send(AuthEffect.LaunchGoogleSignIn)
        }
    }

    private fun resetGoogleSubmission() {
        if (!_state.value.isGoogleSubmitting) return
        _state.update { it.copy(isGoogleSubmitting = false) }
    }

    private fun setGoogleFailure(
        message: String,
    ) {
        _state.update {
            it.copy(
                formError = message,
                isGoogleSubmitting = false,
            )
        }
    }

    private fun submitGoogleIdToken(
        idToken: String,
    ) {
        viewModelScope.launch {
            runCatching {
                authRepository.loginWithGoogle(idToken)
            }.onSuccess { session ->
                completeSession(
                    session = session,
                    onFailure = { message ->
                        _state.update {
                            it.copy(
                                formError = message,
                                isGoogleSubmitting = false,
                            )
                        }
                    },
                    onComplete = {
                        _state.update { it.copy(isGoogleSubmitting = false) }
                    },
                )
            }.onFailure { throwable ->
                val apiException = throwable as? ApiException
                _state.update {
                    it.copy(
                        formError = apiException?.message ?: throwable.toUserMessage(),
                        isGoogleSubmitting = false,
                    )
                }
            }
        }
    }

    private suspend fun completeSession(
        session: AuthSession,
        onFailure: (String) -> Unit,
        onComplete: () -> Unit,
    ) {
        runCatching {
            sessionManager.establishSession(session)
        }.onSuccess {
            onComplete()
        }.onFailure { throwable ->
            onFailure(throwable.toUserMessage())
        }
    }

    private fun applyApiError(
        throwable: Throwable,
    ) {
        val apiException = throwable as? ApiException
        val fieldErrors = apiException?.toAuthFieldErrors() ?: AuthFieldErrors()

        _state.update {
            it.copy(
                emailError = fieldErrors.emailError,
                passwordError = fieldErrors.passwordError,
                usernameError = fieldErrors.usernameError,
                formError = if (fieldErrors.hasAny) {
                    null
                } else {
                    apiException?.message ?: throwable.toUserMessage()
                },
                isSubmitting = false,
                isGoogleSubmitting = false,
            )
        }
    }
}
