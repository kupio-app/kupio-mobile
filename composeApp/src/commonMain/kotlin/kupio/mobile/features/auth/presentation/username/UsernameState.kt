package kupio.mobile.features.auth.presentation.username

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.auth.domain.model.FieldValidationError
import kupio.mobile.features.auth.presentation.auth.AuthFormError

sealed interface UsernameIntent : UiAction {
    data class UsernameChanged(val value: String) : UsernameIntent
    data object SubmitClicked : UsernameIntent
}

data class UsernameState(
    val email: String = "",
    val username: String = "",
    val usernameError: FieldValidationError? = null,
    val formError: AuthFormError? = null,
    val isSubmitting: Boolean = false,
) : UiState
