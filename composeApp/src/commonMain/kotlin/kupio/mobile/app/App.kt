package kupio.mobile.app

import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioTheme
import kupio.mobile.core.di.kupioAppModules
import kupio.mobile.core.navigation.KupioNavigator
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(
        application = {
            modules(kupioAppModules)
        },
    ) {
        KupioTheme {
            KupioNavigator()
        }
    }
}
