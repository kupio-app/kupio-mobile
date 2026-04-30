package kupio.mobile.core.navigation

import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationNavigator {
    // Buffer of 1 ensures if a notification is clicked while the app is still drawing,
    // the event isn't lost before the UI starts collecting it.
    private val _navigationEvents = MutableSharedFlow<Screen>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun navigateTo(screen: Screen) {
        _navigationEvents.tryEmit(screen)
    }
}