package kupio.mobile.features.chats.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.chats.data.remote.ChatApi
import kupio.mobile.features.chats.data.remote.ConversationRoleDto
import kupio.mobile.features.chats.data.remote.ConversationResponseDto
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.repository.ChatsRepository

class ChatsRepositoryImpl(
    private val chatApi: ChatApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : ChatsRepository {

    override suspend fun listConversations(role: ChatRole, limit: Int): List<ConversationData> {
        val dtoRole = when (role) {
            ChatRole.BUYING -> ConversationRoleDto.BUYER
            ChatRole.SELLING -> ConversationRoleDto.SELLER
        }
        return authenticatedApiClient.request { authorize ->
            chatApi.listConversations(authorize, dtoRole, limit)
        }.conversations.map { it.toDomain() }
    }

    override suspend fun startConversation(listingId: String, message: String): ConversationData =
        authenticatedApiClient.request { authorize ->
            chatApi.startConversation(authorize, listingId, message)
        }.toDomain()

    override suspend fun getUnreadCount(): Int =
        authenticatedApiClient.request { authorize ->
            chatApi.getUnreadCount(authorize)
        }.unreadCount

    private fun ConversationResponseDto.toDomain() =
        ConversationData(
            id = id,
            listingId = listingId,
            buyerId = buyerId,
            sellerId = sellerId,
            createdAt = createdAt,
            lastMessagePreview = lastMessagePreview,
            unreadCount = unreadCount,
        )
}
