package kupio.mobile.features.auth.domain

interface SecureSessionStore {
    suspend fun readSession(): AuthSession?

    suspend fun writeSession(
        session: AuthSession,
    )

    suspend fun clear()
}
