package kupio.mobile

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kupio.mobile.core.di.initKoin
import kupio.mobile.core.notifications.PushNotificationManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class KupioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@KupioApplication)
        }

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = true,
            )
        )

        val pushNotificationManager = get<PushNotificationManager>()
        pushNotificationManager.start()
        NotifierManager.addListener(pushNotificationManager)
    }
}
