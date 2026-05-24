package kupio.mobile.debug

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.repository.AuthRepositoryImpl
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.data.repository.TokenRefreshingAuthenticatedApiClient
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import org.koin.dsl.module

internal val testAuthModule = module {
    single<AuthenticatedApiClient> {
        val koin = getKoin()
        TestAuthenticatedApiClient(
            delegate = TokenRefreshingAuthenticatedApiClient(
                authTokenProvider = get(),
                onSessionExpired = { koin.get<AuthSessionManager>().expireSession() },
            ),
            secureSessionStore = get(),
        )
    }

    single<AuthRepository> {
        TestAuthRepository(
            delegate = AuthRepositoryImpl(
                authApi = get<AuthApi>(),
                deviceIdProvider = get(),
                authTokenProvider = get<AuthTokenProvider>(),
                authenticatedApiClient = get(),
            ),
            secureSessionStore = get(),
            cachedAuthenticatedUserStore = get(),
        )
    }
}
