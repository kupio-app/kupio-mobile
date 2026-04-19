package kupio.mobile.features.auth.data

import kupio.mobile.features.auth.domain.AuthRepository
import kupio.mobile.features.auth.domain.AuthSession
import kupio.mobile.features.auth.domain.AuthenticatedUser
import kupio.mobile.features.auth.domain.DeviceIdProvider
import kupio.mobile.features.auth.domain.SecureSessionStore

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val deviceIdProvider: DeviceIdProvider,
    private val secureSessionStore: SecureSessionStore,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthSession {
        return authApi.login(
            request = LoginRequestDto(
                email = email,
                password = password,
                deviceId = deviceIdProvider.getOrCreate(),
            ),
        ).toDomain()
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
    ): AuthSession {
        return authApi.register(
            request = RegisterRequestDto(
                email = email,
                password = password,
                username = username,
                deviceId = deviceIdProvider.getOrCreate(),
            ),
        ).toDomain()
    }

    override suspend fun loginWithGoogle(
        idToken: String,
    ): AuthSession {
        return authApi.loginWithGoogle(
            request = GoogleLoginRequestDto(
                idToken = idToken,
                deviceId = deviceIdProvider.getOrCreate(),
            ),
        ).toDomain()
    }

    override suspend fun refreshSession(): AuthSession {
        val storedSession = requireStoredSession()
        return authApi.refresh(
            request = RefreshRequestDto(
                refreshToken = storedSession.refreshToken,
                deviceId = deviceIdProvider.getOrCreate(),
            ),
        ).toDomain()
    }

    override suspend fun getCurrentUser(): AuthenticatedUser {
        val storedSession = requireStoredSession()
        return authApi.getCurrentUser(
            accessToken = storedSession.accessToken,
        ).toDomain()
    }

    override suspend fun setUsername(
        username: String,
    ): AuthenticatedUser {
        val storedSession = requireStoredSession()
        return authApi.setUsername(
            accessToken = storedSession.accessToken,
            request = SetUsernameRequestDto(username = username),
        ).toDomain()
    }

    override suspend fun clearSession() {
        secureSessionStore.clear()
    }

    private suspend fun requireStoredSession(): AuthSession {
        return requireNotNull(secureSessionStore.readSession()) {
            "No stored auth session is available."
        }
    }
}
