package kupio.mobile.features.chats.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.domain.model.MessageItem

interface ChatsRepository {
    fun observeChats(): StateFlow<List<ChatSummary>>
    fun observeLoading(): StateFlow<Boolean>
    fun observeError(): StateFlow<String?>
    fun observeUnreadCount(): StateFlow<Int>
    suspend fun refresh()
    suspend fun refreshUnreadCount()
    fun observeConversation(id: String): Flow<ChatSummary?>
    suspend fun loadMessages(conversationId: String): List<MessageItem>
    suspend fun sendMessage(conversationId: String, body: String): MessageItem
    suspend fun markSeen(conversationId: String)
}
