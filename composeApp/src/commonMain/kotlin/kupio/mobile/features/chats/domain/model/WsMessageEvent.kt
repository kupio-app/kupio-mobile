package kupio.mobile.features.chats.domain.model

sealed interface WsMessageEvent {
    data class Received(val message: MessageItem) : WsMessageEvent
    data class Deleted(val messageId: String) : WsMessageEvent
    data object TypingStarted : WsMessageEvent
}
