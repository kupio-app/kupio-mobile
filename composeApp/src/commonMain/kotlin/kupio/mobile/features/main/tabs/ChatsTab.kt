package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.chats.ChatsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_chats
import org.jetbrains.compose.resources.stringResource

object ChatsTab : Tab {
    @Composable
    override fun Content() {
        Navigator(ChatsScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = stringResource(Res.string.nav_chats))
}
