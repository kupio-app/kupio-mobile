package kupio.mobile.debug

import kupio.mobile.KupioApplication
import org.koin.core.module.Module

class TestKupioApplication : KupioApplication() {
    override fun additionalKoinModules(): List<Module> = listOf(testAuthModule)
}
