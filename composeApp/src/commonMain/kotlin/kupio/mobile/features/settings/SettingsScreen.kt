package kupio.mobile.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.preferences.ThemeMode
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.settings_logout
import mobile.composeapp.generated.resources.settings_body
import mobile.composeapp.generated.resources.settings_title
import mobile.composeapp.generated.resources.theme_mode_current
import mobile.composeapp.generated.resources.theme_mode_dark
import mobile.composeapp.generated.resources.theme_mode_label
import mobile.composeapp.generated.resources.theme_mode_light
import mobile.composeapp.generated.resources.theme_mode_system
import org.jetbrains.compose.resources.stringResource
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import org.koin.compose.viewmodel.koinViewModel

class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> navigator.pop()
            }
        }

        SettingsRoute(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun SettingsRoute(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    val currentModeLabel = when (state.selectedThemeMode) {
        ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
        ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
    }

    KupioScaffold(title = stringResource(Res.string.settings_title)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            KupioText(text = stringResource(Res.string.settings_body))
            KupioText(text = stringResource(Res.string.theme_mode_label))
            ThemeModeButton(
                text = stringResource(Res.string.theme_mode_system),
                onClick = { onAction(SettingsAction.ThemeModeSelected(ThemeMode.SYSTEM)) },
                enabled = !state.isSigningOut,
            )
            ThemeModeButton(
                text = stringResource(Res.string.theme_mode_light),
                onClick = { onAction(SettingsAction.ThemeModeSelected(ThemeMode.LIGHT)) },
                enabled = !state.isSigningOut,
            )
            ThemeModeButton(
                text = stringResource(Res.string.theme_mode_dark),
                onClick = { onAction(SettingsAction.ThemeModeSelected(ThemeMode.DARK)) },
                enabled = !state.isSigningOut,
            )
            KupioText(
                text = stringResource(Res.string.theme_mode_current, currentModeLabel),
            )
            KupioButton(
                text = stringResource(Res.string.settings_logout),
                onClick = { onAction(SettingsAction.LogoutClicked) },
                enabled = !state.isSigningOut,
            )
            KupioButton(
                text = stringResource(Res.string.back),
                onClick = { onAction(SettingsAction.NavigateBackClicked) },
                enabled = !state.isSigningOut,
            )
        }
    }
}

@Composable
private fun ThemeModeButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    KupioButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

// TODO: Expand this starter settings screen once more app preferences and account settings are introduced.
