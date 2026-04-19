package kupio.mobile.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<HomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                HomeEffect.NavigateToSettings -> navigator.push(SettingsScreen())
            }
        }

        HomeRoute(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun HomeRoute(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    KupioScaffold(title = state.title) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            KupioText(text = state.body)
            KupioText(text = state.note)
            KupioButton(
                text = "Open settings",
                onClick = { onAction(HomeAction.OpenSettingsClicked) },
            )
        }
    }
}

// TODO: Expand the home feature into the real post-auth landing flow once app modules are implemented.
