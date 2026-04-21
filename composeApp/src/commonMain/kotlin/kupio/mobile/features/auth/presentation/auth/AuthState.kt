package kupio.mobile.features.auth.presentation.auth

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.auth.domain.model.FieldValidationError

enum class AuthMode {
    LOGIN,
    REGISTER,
}

sealed interface AuthIntent : UiAction {
    data class ModeSelected(val mode: AuthMode) : AuthIntent
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class ConfirmPasswordChanged(val value: String) : AuthIntent
    data class UsernameChanged(val value: String) : AuthIntent
    data object GoogleClicked : AuthIntent
    data object GoogleCancelled : AuthIntent
    data class GoogleFailure(val errorCode: String) : AuthIntent
    data class GoogleSuccess(val idToken: String) : AuthIntent
    data object SubmitClicked : AuthIntent
}

sealed interface AuthEffect : UiEffect {
    data object LaunchGoogleSignIn : AuthEffect
}

sealed interface AuthFormError {
    data object Generic : AuthFormError
    data object InvalidCredentials : AuthFormError
    data object SessionExpired : AuthFormError
    data class GoogleSignIn(val errorCode: String) : AuthFormError
}

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val emailError: FieldValidationError? = null,
    val passwordError: FieldValidationError? = null,
    val confirmPasswordError: FieldValidationError? = null,
    val usernameError: FieldValidationError? = null,
    val formError: AuthFormError? = null,
    val isSubmitting: Boolean = false,
    val isGoogleSubmitting: Boolean = false,
) : UiState {
    val isBusy: Boolean
        get() = isSubmitting || isGoogleSubmitting
}
