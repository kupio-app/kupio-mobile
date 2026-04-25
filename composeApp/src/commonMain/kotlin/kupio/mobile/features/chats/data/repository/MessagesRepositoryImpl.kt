package kupio.mobile.features.chats.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.data.toItem
import kupio.mobile.features.chats.data.remote.ChatApi
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.repository.MessagesRepository

class MessagesRepositoryImpl(
    private val chatApi: ChatApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val sessionManager: AuthSessionManager,
) : MessagesRepository {

    override suspend fun loadMessages(conversationId: String): List<MessageItem> {
        val currentUserId = currentUserId()
        return authenticatedApiClient.request { authorize ->
            chatApi.listMessages(authorize, conversationId)
        }.messages.reversed().map { it.toItem(currentUserId) }
    }

    override suspend fun sendMessage(conversationId: String, body: String): MessageItem {
        val currentUserId = currentUserId()
        val dto = authenticatedApiClient.request { authorize ->
            chatApi.sendMessage(authorize, conversationId, body)
        }
        return dto.toItem(currentUserId)
    }

    private fun currentUserId() =
        (sessionManager.sessionState.value as? SessionState.SignedIn)?.user?.id.orEmpty()
}
