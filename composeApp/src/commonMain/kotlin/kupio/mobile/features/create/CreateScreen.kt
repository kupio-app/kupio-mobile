package kupio.mobile.features.create

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText

class CreateScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = "Create") {
            KupioText(text = "Create a listing here.")
        }
    }
}
