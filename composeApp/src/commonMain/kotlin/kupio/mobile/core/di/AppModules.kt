package kupio.mobile.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kupio.mobile.core.preferences.DataStorePreferencesRepository
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.createPlatformPreferencesDataStore
import kupio.mobile.features.home.HomeViewModel
import kupio.mobile.features.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val kupioAppModules: List<Module> = listOf(
    module {
        single<DataStore<Preferences>> { createPlatformPreferencesDataStore() }
        single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
        viewModelOf(::HomeViewModel)
        viewModelOf(::SettingsViewModel)
    },
)

// TODO: Split this starter module into feature-specific modules as real data sources and flows are introduced.
