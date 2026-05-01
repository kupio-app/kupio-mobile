package kupio.mobile.core.di

import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.FirebaseAnalyticsService
import org.koin.dsl.module

val analyticsModule = module {
    single<AnalyticsService> { FirebaseAnalyticsService() }
}
