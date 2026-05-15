package kupio.mobile.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.designsystem.KupioTheme
import kupio.mobile.core.navigation.KupioNavigator
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.core.preferences.resolveDarkTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val analytics = koinInject<AnalyticsService>()
    LaunchedEffect(Unit) {
        analytics.logEvent("app_open")
    }

    val preferencesRepository = koinInject<PreferencesRepository>()

    val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle(
        initialValue = ThemeMode.SYSTEM,
    )
    val useDarkTheme = themeMode.resolveDarkTheme(
        systemDarkTheme = isSystemInDarkTheme(),
    )

    KupioTheme(darkTheme = useDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
        ) {
            KupioNavigator()
        }
    }
}
