package kupio.mobile.features.auth.presentation.username

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.validation.AuthValidator
import kupio.mobile.features.auth.presentation.auth.AuthField
import kupio.mobile.features.auth.presentation.auth.mapFieldError
import kupio.mobile.features.auth.presentation.auth.toAuthFormError

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

    fun onIntent(
        intent: UsernameIntent,
    ) {
        when (intent) {
            is UsernameIntent.UsernameChanged -> updateUsername(intent.value)
            UsernameIntent.SubmitClicked -> submit()
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
        if (currentState.isSubmitting) return

        val usernameError = authValidator.validateUsername(currentState.username)
        if (usernameError != null) {
            _state.update { it.copy(usernameError = usernameError) }
            return
        }

        val username = currentState.username.trim()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    formError = null,
                )
            }

            runCatching {
                authRepository.setUsername(username)
            }.onSuccess { user ->
                sessionManager.updateAuthenticatedUser(user)
                _state.update { it.copy(isSubmitting = false) }
            }.onFailure { throwable ->
                if (throwable is AuthSessionExpiredException) {
                    sessionManager.expireSession()
                    _state.update { it.copy(isSubmitting = false) }
                    return@onFailure
                }

                val apiException = throwable as? ApiException
                val mappedError = apiException?.fieldErrors
                    ?.firstOrNull { it.field == "username" }
                    ?.let { error -> mapFieldError(error, AuthField.USERNAME) }

                _state.update {
                    it.copy(
                        usernameError = mappedError,
                        formError = if (mappedError == null) {
                            throwable.toAuthFormError(useInvalidCredentials = false)
                        } else {
                            null
                        },
                        isSubmitting = false,
                    )
                }
            }
        }
    }
}
