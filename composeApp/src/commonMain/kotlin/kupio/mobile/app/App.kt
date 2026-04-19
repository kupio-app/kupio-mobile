package kupio.mobile.app

import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioTheme
import kupio.mobile.features.home.HomeScreen

@Composable
fun App() {
    KupioTheme {
        HomeScreen()
    }
}
