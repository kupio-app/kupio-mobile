package kupio.mobile.core.di

import io.ktor.client.HttpClient
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.core.network.createKupioHttpClient
import kupio.mobile.core.preferences.DataStorePreferencesRepository
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.data.AuthApi
import kupio.mobile.features.auth.data.AuthRepositoryImpl
import kupio.mobile.features.auth.data.DataStoreDeviceIdProvider
import kupio.mobile.features.auth.data.KVaultSecureSessionStore
import kupio.mobile.features.auth.domain.AuthRepository
import kupio.mobile.features.auth.domain.AuthSessionManager
import kupio.mobile.features.auth.domain.AuthValidator
import kupio.mobile.features.auth.domain.DeviceIdProvider
import kupio.mobile.features.auth.domain.SecureSessionStore
import kupio.mobile.features.auth.domain.SessionStateResolver
import kupio.mobile.features.auth.presentation.AuthGateViewModel
import kupio.mobile.features.auth.presentation.AuthViewModel
import kupio.mobile.features.auth.presentation.UsernameViewModel
import kupio.mobile.features.home.HomeViewModel
import kupio.mobile.features.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val kupioAppModules: List<Module> = listOf(
    platformModule,
    module {
        single<HttpClient> {
            createKupioHttpClient(
                baseUrl = get<BackendConfig>().baseUrl,
            )
        }
        single { AuthApi(get()) }
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
        single<SecureSessionStore> { KVaultSecureSessionStore(get()) }
        single<DeviceIdProvider> { DataStoreDeviceIdProvider(get()) }
        single { SessionStateResolver() }
        single { AuthValidator() }
        single { AuthSessionManager(get(), get(), get()) }
        single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
        viewModelOf(::AuthGateViewModel)
        viewModelOf(::AuthViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::UsernameViewModel)
    },
)

// TODO: Split this starter module into feature-specific modules as real data sources and flows are introduced.
