package kupio.mobile.core.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface DeepLinkEvent {
    data class PaymentSuccess(val amountCents: Int) : DeepLinkEvent
}

class DeepLinkNavigator {
    private val _events = Channel<DeepLinkEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun handle(uri: String) {
        val event = parseUri(uri) ?: return
        _events.trySend(event)
    }

    private fun parseUri(uri: String): DeepLinkEvent? = when {
        uri.startsWith("kupio://payment/success") -> {
            val amountCents = uri.substringAfter("amount=", "").toIntOrNull() ?: 0
            DeepLinkEvent.PaymentSuccess(amountCents)
        }
        else -> null
    }
}
