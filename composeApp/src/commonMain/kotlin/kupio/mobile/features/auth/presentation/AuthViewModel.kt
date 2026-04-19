package kupio.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.AuthRepository
import kupio.mobile.features.auth.domain.AuthSessionManager
import kupio.mobile.features.auth.domain.AuthValidator
import kupio.mobile.features.auth.domain.FieldValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val emailError: FieldValidationError? = null,
    val passwordError: FieldValidationError? = null,
    val usernameError: FieldValidationError? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false,
    val isGoogleSubmitting: Boolean = false,
)

sealed interface AuthAction {
    data class ModeSelected(val mode: AuthMode) : AuthAction
    data class EmailChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class UsernameChanged(val value: String) : AuthAction
    data object GoogleClicked : AuthAction
    data object GoogleCancelled : AuthAction
    data class GoogleFailure(val message: String) : AuthAction
    data class GoogleSuccess(val idToken: String) : AuthAction
    data object SubmitClicked : AuthAction
}

sealed interface AuthEffect {
    data object LaunchGoogleSignIn : AuthEffect
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val authValidator: AuthValidator,
    private val sessionManager: AuthSessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<AuthEffect>(Channel.BUFFERED)
    val effects: Flow<AuthEffect> = effectChannel.receiveAsFlow()

    fun onAction(
        action: AuthAction,
    ) {
        when (action) {
            is AuthAction.ModeSelected -> updateMode(action.mode)
            is AuthAction.EmailChanged -> updateEmail(action.value)
            is AuthAction.PasswordChanged -> updatePassword(action.value)
            is AuthAction.UsernameChanged -> updateUsername(action.value)
            AuthAction.GoogleCancelled -> handleGoogleCancelled()
            AuthAction.GoogleClicked -> requestGoogleSignIn()
            is AuthAction.GoogleFailure -> handleGoogleFailure(action.message)
            is AuthAction.GoogleSuccess -> submitGoogleIdToken(action.idToken)
            AuthAction.SubmitClicked -> submit()
        }
    }

    private fun updateMode(
        mode: AuthMode,
    ) {
        if (_state.value.mode == mode) return
        _state.value = _state.value.copy(
            mode = mode,
            emailError = null,
            passwordError = null,
            usernameError = null,
            formError = null,
            isGoogleSubmitting = false,
            isSubmitting = false,
        )
    }

    private fun updateEmail(
        value: String,
    ) {
        _state.value = _state.value.copy(
            email = value,
            emailError = null,
            formError = null,
        )
    }

    private fun updatePassword(
        value: String,
    ) {
        _state.value = _state.value.copy(
            password = value,
            passwordError = null,
            formError = null,
        )
    }

    private fun updateUsername(
        value: String,
    ) {
        _state.value = _state.value.copy(
            username = value,
            usernameError = null,
            formError = null,
        )
    }

    private fun submit() {
        val currentState = _state.value
        if (currentState.isSubmitting || currentState.isGoogleSubmitting) return

        val emailError = authValidator.validateEmail(currentState.email)
        val passwordError = authValidator.validatePassword(currentState.password)
        val usernameError = if (currentState.mode == AuthMode.REGISTER) {
            authValidator.validateUsername(currentState.username)
        } else {
            null
        }

        if (emailError != null || passwordError != null || usernameError != null) {
            _state.value = currentState.copy(
                emailError = emailError,
                passwordError = passwordError,
                usernameError = usernameError,
                formError = null,
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSubmitting = true,
                formError = null,
            )

            runCatching {
                when (_state.value.mode) {
                    AuthMode.LOGIN -> authRepository.login(
                        email = _state.value.email.trim(),
                        password = _state.value.password,
                    )

                    AuthMode.REGISTER -> authRepository.register(
                        email = _state.value.email.trim(),
                        password = _state.value.password,
                        username = _state.value.username.trim(),
                    )
                }
            }.onSuccess { session ->
                runCatching {
                    sessionManager.establishSession(session)
                }.onSuccess {
                    _state.value = _state.value.copy(isSubmitting = false)
                }.onFailure { throwable ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        formError = throwable.toUserMessage(),
                    )
                }
            }.onFailure { throwable ->
                applyApiError(throwable)
            }
        }
    }

    private fun requestGoogleSignIn() {
        val currentState = _state.value
        if (currentState.isSubmitting || currentState.isGoogleSubmitting) return

        _state.value = currentState.copy(
            formError = null,
            isGoogleSubmitting = true,
        )
        viewModelScope.launch {
            effectChannel.send(AuthEffect.LaunchGoogleSignIn)
        }
    }

    private fun handleGoogleCancelled() {
        _state.value = _state.value.copy(
            isGoogleSubmitting = false,
        )
    }

    private fun handleGoogleFailure(
        message: String,
    ) {
        _state.value = _state.value.copy(
            formError = message,
            isGoogleSubmitting = false,
        )
    }

    private fun submitGoogleIdToken(
        idToken: String,
    ) {
        viewModelScope.launch {
            runCatching {
                authRepository.loginWithGoogle(idToken)
            }.onSuccess { session ->
                runCatching {
                    sessionManager.establishSession(session)
                }.onSuccess {
                    _state.value = _state.value.copy(isGoogleSubmitting = false)
                }.onFailure { throwable ->
                    _state.value = _state.value.copy(
                        formError = throwable.toUserMessage(),
                        isGoogleSubmitting = false,
                    )
                }
            }.onFailure { throwable ->
                val apiException = throwable as? ApiException
                _state.value = _state.value.copy(
                    formError = apiException?.message ?: throwable.toUserMessage(),
                    isGoogleSubmitting = false,
                )
            }
        }
    }

    private fun applyApiError(
        throwable: Throwable,
    ) {
        val apiException = throwable as? ApiException
        var emailError: FieldValidationError? = null
        var passwordError: FieldValidationError? = null
        var usernameError: FieldValidationError? = null

        apiException?.fieldErrors?.forEach { error ->
            when (error.field) {
                "email" -> emailError = mapFieldError(error.message, FieldType.EMAIL)
                "password" -> passwordError = mapFieldError(error.message, FieldType.PASSWORD)
                "username" -> usernameError = mapFieldError(error.message, FieldType.USERNAME)
            }
        }

        val hasMappedFieldError = emailError != null || passwordError != null || usernameError != null

        _state.value = _state.value.copy(
            emailError = emailError,
            passwordError = passwordError,
            usernameError = usernameError,
            formError = if (hasMappedFieldError) null else apiException?.message ?: throwable.toUserMessage(),
            isGoogleSubmitting = false,
            isSubmitting = false,
        )
    }

    private fun mapFieldError(
        message: String,
        fieldType: FieldType,
    ): FieldValidationError? {
        val normalized = message.lowercase()
        return when (fieldType) {
            FieldType.EMAIL -> when {
                "required" in normalized -> FieldValidationError.Required
                "email" in normalized -> FieldValidationError.InvalidEmail
                else -> null
            }

            FieldType.PASSWORD -> when {
                "required" in normalized -> FieldValidationError.Required
                "at least 8" in normalized || "min" in normalized -> FieldValidationError.PasswordTooShort
                else -> null
            }

            FieldType.USERNAME -> when {
                "required" in normalized -> FieldValidationError.Required
                "at least 3" in normalized || "too short" in normalized -> FieldValidationError.UsernameTooShort
                "at most 50" in normalized || "too long" in normalized -> FieldValidationError.UsernameTooLong
                else -> null
            }
        }
    }

    private fun Throwable.toUserMessage(): String {
        return message ?: "Something went wrong. Please try again."
    }

    private enum class FieldType {
        EMAIL,
        PASSWORD,
        USERNAME,
    }
}
