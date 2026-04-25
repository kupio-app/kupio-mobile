package kupio.mobile.features.chats.domain.repository

import kupio.mobile.features.chats.domain.model.MessageItem

interface MessagesRepository {
    suspend fun loadMessages(conversationId: String): List<MessageItem>
    suspend fun sendMessage(conversationId: String, body: String): MessageItem
}
