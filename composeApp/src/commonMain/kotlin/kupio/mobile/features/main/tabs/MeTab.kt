package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.me.presentation.profile.MeScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_me
import org.jetbrains.compose.resources.stringResource

object MeTab : Tab {
    @Composable
    override fun Content() {
        Navigator(MeScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = stringResource(Res.string.nav_me))
}
