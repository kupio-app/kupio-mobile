package kupio.mobile.features.auth.domain.session

import kupio.mobile.features.auth.domain.model.AuthSession

interface SecureSessionStore {
    suspend fun readSession(): AuthSession?

    suspend fun writeSession(
        session: AuthSession,
    )

    suspend fun clear()
}
