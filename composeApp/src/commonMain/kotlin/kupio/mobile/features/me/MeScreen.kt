package kupio.mobile.features.me

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.settings.SettingsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.open_settings
import mobile.composeapp.generated.resources.screen_me_body
import mobile.composeapp.generated.resources.topbar_profile_title
import mobile.composeapp.generated.resources.topbar_theme
import org.jetbrains.compose.resources.stringResource

class MeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = stringResource(Res.string.topbar_profile_title),
                    trailingContent = {
                        Icon(
                            modifier = Modifier.bouncingClickable { navigator.push(SettingsScreen()) },
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = stringResource(Res.string.topbar_theme),
                        )
                    },
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.screen_me_body))
            KupioButton(
                text = stringResource(Res.string.open_settings),
                onClick = { navigator.push(SettingsScreen()) },
            )
        }
    }
}
