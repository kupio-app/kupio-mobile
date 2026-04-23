package kupio.mobile.features.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.features.settings.SettingsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.open_settings
import mobile.composeapp.generated.resources.screen_me_body
import mobile.composeapp.generated.resources.topbar_profile_title
import mobile.composeapp.generated.resources.topbar_theme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class MeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<MeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                MeEffect.NavigateToSettings -> navigator.push(SettingsScreen())
            }
        }

        MeRoute(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun MeRoute(
    state: MeState,
    onIntent: (MeIntent) -> Unit,
) {
    val themeToggleIcon = when (state.themeMode) {
        ThemeMode.DARK -> Icons.Outlined.LightMode
        ThemeMode.LIGHT,
        ThemeMode.SYSTEM,
        -> Icons.Outlined.DarkMode
    }

    KupioScaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.topbar_profile_title),
                trailingContent = {
                    KupioTopBarIconAction(
                        imageVector = themeToggleIcon,
                        contentDescription = stringResource(Res.string.topbar_theme),
                        onClick = { onIntent(MeIntent.ThemeToggleClicked) },
                    )
                },
            )
        },
    ) {
        KupioText(text = stringResource(Res.string.screen_me_body))
        KupioButton(
            text = stringResource(Res.string.open_settings),
            onClick = { onIntent(MeIntent.OpenSettingsClicked) },
        )
    }
}
