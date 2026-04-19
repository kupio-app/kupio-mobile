package kupio.mobile.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kupio.mobile.features.auth.domain.AuthSessionManager
import kupio.mobile.features.auth.domain.SessionState

class AuthGateViewModel(
    private val sessionManager: AuthSessionManager,
) : ViewModel() {
    val state: StateFlow<SessionState> = sessionManager.sessionState

    init {
        viewModelScope.launch {
            sessionManager.bootstrap()
        }
    }
}
