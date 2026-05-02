package kupio.mobile.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioCenteredContent
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.presentation.auth.AuthScreen
import kupio.mobile.features.auth.presentation.username.UsernameScreen
import kupio.mobile.features.main.MainTabsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_bootstrap_failed
import mobile.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KupioNavigator() {
    val viewModel = koinViewModel<RootNavigationViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        SessionState.Loading -> KupioCenteredContent {
            CircularProgressIndicator()
        }

        SessionState.BootstrapFailed -> KupioCenteredContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
            ) {
                KupioText(text = stringResource(Res.string.auth_bootstrap_failed))
                KupioButton(
                    text = stringResource(Res.string.retry),
                    onClick = viewModel::retryBootstrap,
                )
            }
        }

        SessionState.SignedOut -> key(SessionState.SignedOut::class) { KupioDefaultNavigator(AuthScreen()) }
        is SessionState.NeedsUsername -> key(current::class) { KupioDefaultNavigator(UsernameScreen()) }
        is SessionState.SignedIn -> key(current::class) { KupioDefaultNavigator(MainTabsScreen()) }
    }
}

@Composable
private fun KupioDefaultNavigator(screen: Screen) {
    val notificationNavigator = koinInject<NotificationNavigator>()
    val analytics = koinInject<AnalyticsService>()

    Navigator(screen) { navigator ->
        val currentScreen = navigator.lastItem
        LaunchedEffect(currentScreen) {
            val screenClass = currentScreen::class.simpleName ?: "UnknownScreen"
            analytics.logEvent("screen_view", mapOf(
                "screen_name" to screenClass,
                "screen_class" to screenClass
            ))
        }

        // Listen to the Event Bus for Deep Links
        LaunchedEffect(Unit) {
            notificationNavigator.navigationEvents.collect { targetScreen ->
                navigator.push(targetScreen)
            }
        }

        CurrentScreen()
    }
}