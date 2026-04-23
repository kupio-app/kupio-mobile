package kupio.mobile.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.settings_body
import mobile.composeapp.generated.resources.settings_logout
import mobile.composeapp.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource
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
    KupioScaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.settings_title),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.back),
                        onClick = { onAction(SettingsAction.NavigateBackClicked) },
                    )
                },
            )
        },
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            KupioText(text = stringResource(Res.string.settings_body))
            SettingsThemeModeSection(
                selectedThemeMode = state.selectedThemeMode,
                enabled = !state.isSigningOut,
                onModeSelected = { onAction(SettingsAction.ThemeModeSelected(it)) },
            )
            KupioButton(
                text = stringResource(Res.string.settings_logout),
                onClick = { onAction(SettingsAction.LogoutClicked) },
                enabled = !state.isSigningOut,
            )
        }
    }
}

// TODO: Expand this starter settings screen once more app preferences and account settings are introduced.
