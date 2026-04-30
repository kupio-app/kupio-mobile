package kupio.mobile.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmk.kmpnotifier.notification.NotifierManager
import kupio.mobile.core.designsystem.KupioTheme
import kupio.mobile.core.navigation.KupioNavigator
import kupio.mobile.core.navigation.NotificationNavigator
import kupio.mobile.core.notifications.PushNotificationManager
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.core.preferences.resolveDarkTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val preferencesRepository = koinInject<PreferencesRepository>()

    val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle(
        initialValue = ThemeMode.SYSTEM,
    )
    val useDarkTheme = themeMode.resolveDarkTheme(
        systemDarkTheme = isSystemInDarkTheme(),
    )

    KupioTheme(darkTheme = useDarkTheme) {
        KupioNavigator()
    }
}
