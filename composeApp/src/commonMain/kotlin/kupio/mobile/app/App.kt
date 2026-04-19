package kupio.mobile.app

import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioTheme
import kupio.mobile.core.navigation.KupioNavigator

@Composable
fun App() {
    KupioTheme {
        KupioNavigator()
    }
}
