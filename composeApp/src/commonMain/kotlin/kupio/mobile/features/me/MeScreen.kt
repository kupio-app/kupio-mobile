package kupio.mobile.features.me

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.features.settings.SettingsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_me
import mobile.composeapp.generated.resources.open_settings
import mobile.composeapp.generated.resources.screen_me_body
import org.jetbrains.compose.resources.stringResource

class MeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        KupioScaffold(title = stringResource(Res.string.nav_me)) {
            KupioText(text = stringResource(Res.string.screen_me_body))
            KupioButton(
                text = stringResource(Res.string.open_settings),
                onClick = { navigator.push(SettingsScreen()) },
            )
        }
    }
}
