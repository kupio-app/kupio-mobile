package kupio.mobile.features.auth.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.remote.RefreshRequestDto
import kupio.mobile.features.auth.data.remote.toDomain
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider
import kupio.mobile.features.auth.domain.session.SecureSessionStore

private const val AccessTokenRefreshSkewSeconds = 60L

class AuthTokenProvider(
    private val authApi: AuthApi,
    private val deviceIdProvider: DeviceIdProvider,
    private val secureSessionStore: SecureSessionStore,
    private val clock: AuthClock,
) {
    private val refreshMutex = Mutex()

    suspend fun freshAccessToken(): String {
        val session = requireSession()
        if (session.refreshExpiresAt <= clock.nowEpochSeconds()) {
            expireSession()
        }
        if (!session.requiresAccessRefresh()) {
            return session.accessToken
        }

        return refreshMutex.withLock {
            val lockedSession = requireSession()
            if (lockedSession.refreshExpiresAt <= clock.nowEpochSeconds()) {
                expireSession()
            }
            if (!lockedSession.requiresAccessRefresh()) {
                return@withLock lockedSession.accessToken
            }

            refreshSessionLocked(lockedSession.refreshToken).accessToken
        }
    }

    suspend fun refreshSession(): AuthSession {
        return refreshMutex.withLock {
            val session = requireSession()
            if (session.refreshExpiresAt <= clock.nowEpochSeconds()) {
                expireSession()
            }
            refreshSessionLocked(session.refreshToken)
        }
    }

    suspend fun refreshAfterUnauthorized(
        failedAccessToken: String,
    ): String {
        return refreshMutex.withLock {
            val session = requireSession()
            if (session.refreshExpiresAt <= clock.nowEpochSeconds()) {
                expireSession()
            }
            if (session.accessToken != failedAccessToken && !session.requiresAccessRefresh()) {
                return@withLock session.accessToken
            }

            refreshSessionLocked(session.refreshToken).accessToken
        }
    }

    suspend fun clearSession() {
        secureSessionStore.clear()
    }

    private suspend fun refreshSessionLocked(
        refreshToken: String,
    ): AuthSession {
        return try {
            authApi.refresh(
                request = RefreshRequestDto(
                    refreshToken = refreshToken,
                    deviceId = deviceIdProvider.getOrCreate(),
                ),
            ).toDomain().also { session ->
                secureSessionStore.writeSession(session)
            }
        } catch (throwable: Throwable) {
            if (throwable is ApiException && throwable.statusCode == 401) {
                secureSessionStore.clear()
                throw AuthSessionExpiredException()
            }
            throw throwable
        }
    }

    private suspend fun requireSession(): AuthSession {
        return secureSessionStore.readSession() ?: throw AuthSessionExpiredException()
    }

    private suspend fun expireSession(): Nothing {
        secureSessionStore.clear()
        throw AuthSessionExpiredException()
    }

    private fun AuthSession.requiresAccessRefresh(): Boolean {
        return accessExpiresAt <= clock.nowEpochSeconds() + AccessTokenRefreshSkewSeconds
    }
}
