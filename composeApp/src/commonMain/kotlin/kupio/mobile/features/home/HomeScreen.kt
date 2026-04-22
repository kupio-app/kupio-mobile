package kupio.mobile.features.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_home
import mobile.composeapp.generated.resources.screen_home_body
import org.jetbrains.compose.resources.stringResource

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        KupioScaffold(title = stringResource(Res.string.nav_home)) {
            KupioText(text = stringResource(Res.string.screen_home_body))
        }
    }
}
