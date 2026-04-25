package kupio.mobile.features.chats.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.chats.data.remote.ChatApi
import kupio.mobile.features.chats.data.remote.ConversationRoleDto
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.repository.ChatsRepository

class ChatsRepositoryImpl(
    private val chatApi: ChatApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : ChatsRepository {

    override suspend fun listConversations(role: ChatRole, limit: Int): List<ConversationData> {
        val dtoRole = when (role) {
            ChatRole.BUYING -> ConversationRoleDto.SELLER
            ChatRole.SELLING -> ConversationRoleDto.BUYER
        }
        return authenticatedApiClient.request { authorize ->
            chatApi.listConversations(authorize, dtoRole, limit)
        }.conversations.map { conv ->
            ConversationData(
                id = conv.id,
                listingId = conv.listingId,
                buyerId = conv.buyerId,
                sellerId = conv.sellerId,
                createdAt = conv.createdAt,
                lastMessagePreview = conv.lastMessagePreview,
                unreadCount = conv.unreadCount,
            )
        }
    }

    override suspend fun getUnreadCount(): Int =
        authenticatedApiClient.request { authorize ->
            chatApi.getUnreadCount(authorize)
        }.unreadCount
}
