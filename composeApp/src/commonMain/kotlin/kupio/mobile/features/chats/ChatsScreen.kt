package kupio.mobile.features.chats

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText

class ChatsScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = "Chats") {
            KupioText(text = "Your conversations will appear here.")
        }
    }
}
