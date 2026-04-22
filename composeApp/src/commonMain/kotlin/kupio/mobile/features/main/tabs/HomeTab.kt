package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.home.HomeScreen

object HomeTab : Tab {
    @Composable
    override fun Content() {
        Navigator(HomeScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "Home")
}
