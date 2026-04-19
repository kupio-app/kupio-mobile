package kupio.mobile.core.di

import kupio.mobile.features.home.HomeContentRepository
import kupio.mobile.features.home.HomeViewModel
import kupio.mobile.features.home.InMemoryHomeContentRepository
import kupio.mobile.features.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val kupioAppModules: List<Module> = listOf(
    module {
        single<HomeContentRepository> {
            InMemoryHomeContentRepository()
        }
        viewModelOf(::HomeViewModel)
        viewModelOf(::SettingsViewModel)
    },
)

// TODO: Split this starter module into feature-specific modules as real data sources and flows are introduced.
