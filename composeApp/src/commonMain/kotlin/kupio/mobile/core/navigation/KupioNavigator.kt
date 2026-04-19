package kupio.mobile.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import kupio.mobile.core.designsystem.KupioCenteredContent
import kupio.mobile.features.auth.domain.SessionState
import kupio.mobile.features.auth.presentation.AuthGateViewModel
import kupio.mobile.features.auth.presentation.AuthScreen
import kupio.mobile.features.auth.presentation.UsernameScreen
import kupio.mobile.features.home.HomeScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KupioNavigator() {
    val viewModel = koinViewModel<AuthGateViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        SessionState.Loading -> KupioCenteredContent {
            androidx.compose.material3.CircularProgressIndicator()
        }

        SessionState.SignedOut -> Navigator(AuthScreen())
        is SessionState.NeedsUsername -> Navigator(UsernameScreen())
        is SessionState.SignedIn -> Navigator(HomeScreen())
    }
}
