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
            val amountCents = queryParam(uri, "amount")?.toIntOrNull()?.takeIf { it > 0 }
                ?: return null
            DeepLinkEvent.PaymentSuccess(amountCents)
        }
        else -> null
    }

    private fun queryParam(uri: String, key: String): String? {
        val query = uri.substringAfter("?", "")
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
    }
}
