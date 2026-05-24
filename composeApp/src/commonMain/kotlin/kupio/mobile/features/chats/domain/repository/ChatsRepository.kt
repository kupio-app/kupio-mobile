package kupio.mobile.features.chats.domain.repository

import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.model.ConversationsPage

interface ChatsRepository {
    suspend fun listConversations(role: ChatRole, limit: Int = 50): List<ConversationData>
    suspend fun listConversationsPage(role: ChatRole, limit: Int = 20, cursor: String? = null): ConversationsPage
    suspend fun startConversation(listingId: String, message: String): ConversationData
    suspend fun getUnreadCount(): Int
}
