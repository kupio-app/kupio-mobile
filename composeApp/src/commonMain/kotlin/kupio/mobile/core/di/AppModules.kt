package kupio.mobile.core.di

import kupio.mobile.core.preferences.DataStorePreferencesRepository
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.home.HomeViewModel
import kupio.mobile.features.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val kupioAppModules: List<Module> = listOf(
    platformModule,
    module {
        single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
        viewModelOf(::HomeViewModel)
        viewModelOf(::SettingsViewModel)
    },
)

// TODO: Split this starter module into feature-specific modules as real data sources and flows are introduced.
