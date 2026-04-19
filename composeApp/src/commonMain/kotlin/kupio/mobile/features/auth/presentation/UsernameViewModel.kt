package kupio.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.AuthRepository
import kupio.mobile.features.auth.domain.AuthSessionManager
import kupio.mobile.features.auth.domain.AuthValidator
import kupio.mobile.features.auth.domain.FieldValidationError
import kupio.mobile.features.auth.domain.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsernameState(
    val email: String = "",
    val username: String = "",
    val usernameError: FieldValidationError? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false,
)

sealed interface UsernameAction {
    data class UsernameChanged(val value: String) : UsernameAction
    data object SubmitClicked : UsernameAction
}

class UsernameViewModel(
    private val authRepository: AuthRepository,
    private val authValidator: AuthValidator,
    private val sessionManager: AuthSessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(
        UsernameState(
            email = (sessionManager.sessionState.value as? SessionState.NeedsUsername)?.user?.email.orEmpty(),
        ),
    )
    val state = _state.asStateFlow()

    fun onAction(
        action: UsernameAction,
    ) {
        when (action) {
            is UsernameAction.UsernameChanged -> updateUsername(action.value)
            UsernameAction.SubmitClicked -> submit()
        }
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
        if (_state.value.isSubmitting) return

        val usernameError = authValidator.validateUsername(_state.value.username)
        if (usernameError != null) {
            _state.value = _state.value.copy(usernameError = usernameError)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSubmitting = true,
                formError = null,
            )

            runCatching {
                authRepository.setUsername(_state.value.username.trim())
            }.onSuccess { user ->
                sessionManager.updateAuthenticatedUser(user)
                _state.value = _state.value.copy(isSubmitting = false)
            }.onFailure { throwable ->
                val apiException = throwable as? ApiException
                val usernameError = apiException?.fieldErrors
                    ?.firstOrNull { it.field == "username" }
                    ?.message
                    ?.let(::mapUsernameError)
                _state.value = _state.value.copy(
                    usernameError = usernameError,
                    formError = if (usernameError != null) {
                        null
                    } else {
                        apiException?.message ?: throwable.message ?: "Unable to save username."
                    },
                    isSubmitting = false,
                )
            }
        }
    }

    private fun mapUsernameError(
        message: String,
    ): FieldValidationError? {
        val normalized = message.lowercase()
        return when {
            "required" in normalized -> FieldValidationError.Required
            "at least 3" in normalized || "too short" in normalized -> FieldValidationError.UsernameTooShort
            "at most 50" in normalized || "too long" in normalized -> FieldValidationError.UsernameTooLong
            else -> null
        }
    }
}
