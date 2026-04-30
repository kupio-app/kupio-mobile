package kupio.mobile.core.notifications

import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kupio.mobile.core.navigation.NotificationNavigator
import kupio.mobile.core.notifications.data.DeepLinksType
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.presentation.thread.ChatThreadScreen

class PushNotificationManager(
    private val navigator: NotificationNavigator,
    private val preferences: PreferencesRepository,
    private val notifRepository: NotificationsRepository,
    private val sessionManager: AuthSessionManager
) : NotifierManager.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            sessionManager.sessionState
                .filterIsInstance<SessionState.SignedIn>()
                .collect {
                    preferences.pushToken.single()?.let {
                        notifRepository.sendPushToken(it)
                    }
                }
        }
    }

    override fun onNewToken(token: String) {
        preferences.savePushToken(token)

        if (sessionManager.sessionState.value is SessionState.SignedIn) {
            scope.launch { notifRepository.sendPushToken(token) }
        }
    }

    override fun onPayloadData(data: PayloadData) {
        println("Foreground Payload Received: $data")
    }

    override fun onNotificationClicked(data: PayloadData) {
        val type = DeepLinksType.from(data["type"] as String?)
        when (type) {
            DeepLinksType.ChatMessage -> {
                val conversationId = data["conversationId"] as String?
                conversationId?.let {
                    navigator.navigateTo(ChatThreadScreen(it))
                }
            }
            null -> println("Unknown notification type: ${data["type"]}")
        }
    }
}