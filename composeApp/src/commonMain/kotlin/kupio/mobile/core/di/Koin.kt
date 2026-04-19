package kupio.mobile.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
) {
    val defaultContext = KoinPlatformTools.defaultContext()
    if (defaultContext.getOrNull() != null) return

    startKoin {
        appDeclaration()
        modules(kupioAppModules)
    }
}
