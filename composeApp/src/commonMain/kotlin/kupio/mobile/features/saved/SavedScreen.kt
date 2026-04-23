package kupio.mobile.features.saved

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioTopNavbar
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_saved
import mobile.composeapp.generated.resources.screen_saved_body
import org.jetbrains.compose.resources.stringResource

class SavedScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = stringResource(Res.string.nav_saved),
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.screen_saved_body))
        }
    }
}
