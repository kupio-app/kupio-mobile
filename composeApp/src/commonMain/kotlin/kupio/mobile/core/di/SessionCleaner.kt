package kupio.mobile.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager

class SessionCleaner(
    private val sessionManager: AuthSessionManager,
    applicationScope: CoroutineScope,
) {
    private val handlers = mutableListOf<() -> Unit>()

    init {
        applicationScope.launch {
            sessionManager.sessionState
                .filterIsInstance<SessionState.SignedOut>()
                .collect { handlers.forEach { it() } }
        }
    }

    fun register(onClear: () -> Unit) {
        handlers.add(onClear)
    }
}
