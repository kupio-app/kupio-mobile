package kupio.mobile.features.auth.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSessionManager(
    private val authRepository: AuthRepository,
    private val secureSessionStore: SecureSessionStore,
    private val sessionStateResolver: SessionStateResolver,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    suspend fun bootstrap() {
        _sessionState.value = SessionState.Loading
        val storedSession = secureSessionStore.readSession()
        if (storedSession == null) {
            _sessionState.value = SessionState.SignedOut
            return
        }

        try {
            val refreshedSession = authRepository.refreshSession()
            establishSession(refreshedSession)
        } catch (_: Throwable) {
            authRepository.clearSession()
            secureSessionStore.clear()
            _sessionState.value = SessionState.SignedOut
        }
    }

    suspend fun establishSession(
        session: AuthSession,
    ) {
        secureSessionStore.writeSession(session)
        val user = runCatching {
            authRepository.getCurrentUser()
        }.getOrElse { throwable ->
            authRepository.clearSession()
            secureSessionStore.clear()
            _sessionState.value = SessionState.SignedOut
            throw throwable
        }

        _sessionState.value = sessionStateResolver.resolve(user)
    }

    suspend fun updateAuthenticatedUser(
        user: AuthenticatedUser,
    ) {
        _sessionState.value = sessionStateResolver.resolve(user)
    }

    suspend fun signOut() {
        authRepository.clearSession()
        secureSessionStore.clear()
        _sessionState.value = SessionState.SignedOut
    }
}
