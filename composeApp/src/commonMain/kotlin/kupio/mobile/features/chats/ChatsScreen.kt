package kupio.mobile.features.chats

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.bouncingClickable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_chats
import mobile.composeapp.generated.resources.screen_chats_body
import mobile.composeapp.generated.resources.topbar_search
import mobile.composeapp.generated.resources.topbar_unread_messages
import org.jetbrains.compose.resources.stringResource

class ChatsScreen : Screen {
    @Composable
    override fun Content() {
        val unreadCount = 0
        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = stringResource(Res.string.nav_chats),
                    subtitle = stringResource(Res.string.topbar_unread_messages, unreadCount),
                    trailingContent = {
                        Icon(
                            modifier = Modifier.bouncingClickable {  },
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(Res.string.topbar_search),
                        )
                    },
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.screen_chats_body))
        }
    }
}
