package kupio.mobile.features.main.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kupio.mobile.features.chats.ChatsScreen

object ChatsTab : Tab {
    @Composable
    override fun Content() {
        Navigator(ChatsScreen())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "Chats")
}
