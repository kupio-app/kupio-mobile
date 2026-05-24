package kupio.mobile.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import mobile.composeapp.generated.resources.settings_user_id_label
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
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings.screen"),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            KupioText(text = stringResource(Res.string.settings_body))
            if (state.userId.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.settings_user_id_label, state.userId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("settings.user-id"),
                )
            }
            SettingsThemeModeSection(
                selectedThemeMode = state.selectedThemeMode,
                enabled = !state.isSigningOut,
                onModeSelected = { onAction(SettingsAction.ThemeModeSelected(it)) },
            )
            KupioButton(
                text = stringResource(Res.string.settings_logout),
                onClick = { onAction(SettingsAction.LogoutClicked) },
                enabled = !state.isSigningOut,
                modifier = Modifier.testTag("settings.logout"),
            )
        }
    }
}

// TODO: Expand this starter settings screen once more app preferences and account settings are introduced.
