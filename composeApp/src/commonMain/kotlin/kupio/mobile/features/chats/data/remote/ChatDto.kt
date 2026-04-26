package kupio.mobile.features.chats.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ConversationRoleDto {
    @SerialName("buyer") BUYER,
    @SerialName("seller") SELLER,
}

@Serializable
data class ConversationResponseDto(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class UnreadCountResponseDto(
    @SerialName("unread_count") val unreadCount: Int,
)

@Serializable
data class ListConversationsResponseDto(
    val conversations: List<ConversationResponseDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class MessageResponseDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("created_at") val createdAt: String,
    val content: String? = null,
)

@Serializable
data class ListMessagesResponseDto(
    val messages: List<MessageResponseDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class SendMessageRequestDto(
    val body: String,
)
