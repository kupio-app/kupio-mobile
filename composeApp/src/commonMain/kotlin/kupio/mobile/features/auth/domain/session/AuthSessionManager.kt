package kupio.mobile.features.auth.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.repository.AuthRepository

class AuthSessionManager(
    private val authRepository: AuthRepository,
    private val secureSessionStore: SecureSessionStore,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    private val sessionMutex = Mutex()

    suspend fun bootstrap() {
        sessionMutex.withLock {
            _sessionState.value = SessionState.Loading
            val storedSession = secureSessionStore.readSession()
            if (storedSession == null) {
                _sessionState.value = SessionState.SignedOut
                return
            }

            try {
                val refreshedSession = authRepository.refreshSession()
                establishSessionLocked(refreshedSession)
            } catch (_: AuthSessionExpiredException) {
                clearPersistedSession()
            } catch (_: Throwable) {
                _sessionState.value = SessionState.BootstrapFailed
            }
        }
    }

    suspend fun establishSession(session: AuthSession) {
        sessionMutex.withLock {
            establishSessionLocked(session)
        }
    }

    suspend fun updateAuthenticatedUser(user: AuthenticatedUser) {
        sessionMutex.withLock {
            _sessionState.value = user.toSessionState()
        }
    }

    suspend fun signOut() {
        sessionMutex.withLock {
            val refreshToken = secureSessionStore.readSession()?.refreshToken
            clearPersistedSession()
            refreshToken?.let { token ->
                runCatching {
                    authRepository.logout(refreshToken = token)
                }
            }
        }
    }

    suspend fun expireSession() {
        sessionMutex.withLock {
            clearPersistedSession()
        }
    }

    private suspend fun establishSessionLocked(session: AuthSession) {
        secureSessionStore.writeSession(session)
        val user = runCatching {
            authRepository.getCurrentUser()
        }.getOrElse { throwable ->
            if (throwable is AuthSessionExpiredException) {
                clearPersistedSession()
            }
            throw throwable
        }
        _sessionState.value = user.toSessionState()
    }

    private suspend fun clearPersistedSession() {
        secureSessionStore.clear()
        _sessionState.value = SessionState.SignedOut
    }
}
