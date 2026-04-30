package kupio.mobile.features.chats.domain.repository

import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData

interface ChatsRepository {
    suspend fun listConversations(role: ChatRole, limit: Int = 50): List<ConversationData>
    suspend fun startConversation(listingId: String, message: String): ConversationData
    suspend fun getUnreadCount(): Int
}
