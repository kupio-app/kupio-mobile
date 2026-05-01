package kupio.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kupio.mobile.app.App
import kupio.mobile.core.di.initKoin
import kupio.mobile.core.notifications.BackgroundSyncScheduler
import kupio.mobile.core.notifications.PushNotificationManager
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()

    NotifierManager.initialize(
        configuration = NotificationPlatformConfiguration.Ios(
            showPushNotification = true,
            askNotificationPermissionOnStart = false,
        )
    )

    val koin = KoinPlatformTools.defaultContext().get()
    val pushNotificationManager = koin.get<PushNotificationManager>()
    pushNotificationManager.start()
    NotifierManager.addListener(pushNotificationManager)

    koin.get<BackgroundSyncScheduler>().schedule()

    return ComposeUIViewController { App() }
}
