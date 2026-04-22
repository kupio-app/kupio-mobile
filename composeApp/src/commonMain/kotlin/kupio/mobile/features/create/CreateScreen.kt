package kupio.mobile.features.create

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_create
import mobile.composeapp.generated.resources.screen_create_body
import org.jetbrains.compose.resources.stringResource

class CreateScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = stringResource(Res.string.nav_create)) {
            KupioText(text = stringResource(Res.string.screen_create_body))
        }
    }
}
