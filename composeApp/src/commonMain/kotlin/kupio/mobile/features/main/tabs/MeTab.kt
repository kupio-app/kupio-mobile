package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.me.MeScreen

object MeTab : Tab {
    @Composable
    override fun Content() {
        Navigator(MeScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "Me")
}
