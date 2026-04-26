package kupio.mobile.features.chats.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kupio.mobile.core.network.bodyOrThrow
import kupio.mobile.core.network.toApiException

class ChatApi(private val httpClient: HttpClient) {

    suspend fun listConversations(
        authorize: HttpRequestBuilder.() -> Unit,
        role: ConversationRoleDto,
        limit: Int = 50,
        cursor: String? = null,
    ): ListConversationsResponseDto = httpClient.get("/api/chat/conversations") {
        authorize()
        url {
            parameters.append("role", role.name.lowercase())
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow()

    suspend fun getConversation(
        authorize: HttpRequestBuilder.() -> Unit,
        conversationId: String,
    ): ConversationResponseDto = httpClient.get("/api/chat/conversations/$conversationId") {
        authorize()
    }.bodyOrThrow()

    suspend fun listMessages(
        authorize: HttpRequestBuilder.() -> Unit,
        conversationId: String,
        limit: Int = 50,
        cursor: String? = null,
    ): ListMessagesResponseDto = httpClient.get("/api/chat/conversations/$conversationId/messages") {
        authorize()
        url {
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow()

    suspend fun sendMessage(
        authorize: HttpRequestBuilder.() -> Unit,
        conversationId: String,
        body: String,
    ): MessageResponseDto = httpClient.post("/api/chat/conversations/$conversationId/messages") {
        authorize()
        setBody(SendMessageRequestDto(body = body))
    }.bodyOrThrow()

    suspend fun getUnreadCount(
        authorize: HttpRequestBuilder.() -> Unit,
    ): UnreadCountResponseDto = httpClient.get("/api/chat/conversations/unread-count") {
        authorize()
    }.bodyOrThrow()

    suspend fun deleteMessage(
        authorize: HttpRequestBuilder.() -> Unit,
        conversationId: String,
        messageId: String,
    ) {
        val response = httpClient.delete("/api/chat/conversations/$conversationId/messages/$messageId") {
            authorize()
        }
        if (response.status.value !in 200..299) throw response.toApiException()
    }
}
