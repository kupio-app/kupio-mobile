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
import mobile.composeapp.generated.resources.open_settings
import org.jetbrains.compose.resources.stringResource

class MeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        KupioScaffold(title = "Me") {
            KupioText(text = "Your profile and preferences will appear here.")
            KupioButton(
                text = stringResource(Res.string.open_settings),
                onClick = { navigator.push(SettingsScreen()) },
            )
        }
    }
}
