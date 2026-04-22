package kupio.mobile.features.saved

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText

class SavedScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = "Saved") {
            KupioText(text = "Your saved listings will appear here.")
        }
    }
}
