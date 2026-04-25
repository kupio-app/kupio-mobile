package kupio.mobile.features.chats.domain.repository

import kotlinx.coroutines.flow.Flow
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.WsMessageEvent

interface MessagesRepository {
    suspend fun loadMessages(conversationId: String): List<MessageItem>
    suspend fun sendMessage(conversationId: String, body: String): MessageItem
    fun observeMessages(conversationId: String): Flow<WsMessageEvent>
}
