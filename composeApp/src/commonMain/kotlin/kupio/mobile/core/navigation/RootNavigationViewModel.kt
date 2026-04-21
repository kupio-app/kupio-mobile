package kupio.mobile.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager

class RootNavigationViewModel(
    private val sessionManager: AuthSessionManager,
) : ViewModel() {
    val state: StateFlow<SessionState> = sessionManager.sessionState

    init {
        retryBootstrap()
    }

    fun retryBootstrap() {
        viewModelScope.launch {
            sessionManager.bootstrap()
        }
    }
}
