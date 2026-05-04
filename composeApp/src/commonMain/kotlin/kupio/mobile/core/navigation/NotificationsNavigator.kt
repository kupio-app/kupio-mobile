package kupio.mobile.core.navigation

import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class NotificationNavigator {
    private val _navigationEvents = Channel<Screen>(capacity = Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun navigateTo(screen: Screen) {
        _navigationEvents.trySend(screen)
    }
}
