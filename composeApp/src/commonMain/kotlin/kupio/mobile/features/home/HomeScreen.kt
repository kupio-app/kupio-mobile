package kupio.mobile.features.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = "Home") {
            KupioText(text = "Your feed will appear here.")
        }
    }
}
