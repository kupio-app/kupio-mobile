package kupio.mobile.core.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import kupio.mobile.features.home.HomeScreen

@Composable
fun KupioNavigator() {
    // TODO: Replace the starter navigator root with the real app graph once auth and main flows exist.
    Navigator(HomeScreen())
}
