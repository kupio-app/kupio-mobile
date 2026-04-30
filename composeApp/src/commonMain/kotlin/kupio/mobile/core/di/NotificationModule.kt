package kupio.mobile.core.di

import org.koin.dsl.module
import kupio.mobile.core.navigation.NotificationNavigator
import kupio.mobile.core.notifications.NotificationsRepository
import kupio.mobile.core.notifications.PushNotificationManager
import kupio.mobile.core.notifications.data.NotificationsApi
import kupio.mobile.core.notifications.data.NotificationsRepositoryImpl

val notificationModule = module {
    single { NotificationsApi(get()) }
    single<NotificationsRepository> { NotificationsRepositoryImpl(get(), get()) }
    single { NotificationNavigator() }
    single { PushNotificationManager(get(), get(), get(), get()) }
}