package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.home.presentation.HomeScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_home
import org.jetbrains.compose.resources.stringResource

object HomeTab : Tab {
    @Composable
    override fun Content() {
        Navigator(HomeScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = stringResource(Res.string.nav_home))
}
