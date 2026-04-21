package kupio.mobile.core.di

import io.ktor.client.HttpClient
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.network.createKupioHttpClient
import kupio.mobile.core.preferences.DataStorePreferencesRepository
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.data.local.DataStoreDeviceIdProvider
import kupio.mobile.features.auth.data.local.KVaultSecureSessionStore
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.repository.AuthRepositoryImpl
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.data.repository.AuthClock
import kupio.mobile.features.auth.data.repository.SystemAuthClock
import kupio.mobile.features.auth.data.repository.TokenRefreshingAuthenticatedApiClient
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import kupio.mobile.features.auth.domain.validation.AuthValidator
import kupio.mobile.features.auth.presentation.auth.AuthViewModel
import kupio.mobile.features.auth.presentation.username.UsernameViewModel
import kupio.mobile.features.home.HomeViewModel
import kupio.mobile.features.settings.SettingsViewModel
import kupio.mobile.core.navigation.RootNavigationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val kupioAppModules: List<Module> = listOf(
    platformModule,
    module {
        single<HttpClient> {
            val config = get<BackendConfig>()
            createKupioHttpClient(
                baseUrl = config.baseUrl,
                isDebug = config.isDebug,
            )
        }
        single { AuthApi(get()) }
        single<AuthClock> { SystemAuthClock() }
        single { AuthTokenProvider(get(), get(), get(), get()) }
        single<AuthenticatedApiClient> {
            val koin = getKoin()
            TokenRefreshingAuthenticatedApiClient(
                authTokenProvider = get(),
                onSessionExpired = { koin.get<AuthSessionManager>().expireSession() },
            )
        }
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
        single<SecureSessionStore> { KVaultSecureSessionStore(get()) }
        single<DeviceIdProvider> { DataStoreDeviceIdProvider(get()) }
        single { AuthValidator() }
        single { AuthSessionManager(get(), get()) }
        single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
        viewModelOf(::RootNavigationViewModel)
        viewModelOf(::AuthViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::UsernameViewModel)
    },
)
