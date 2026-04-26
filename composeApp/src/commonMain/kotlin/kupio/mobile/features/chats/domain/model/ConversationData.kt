package kupio.mobile.features.chats.domain.model

data class ConversationData(
    val id: String,
    val listingId: String,
    val buyerId: String,
    val sellerId: String,
    val createdAt: String,
    val lastMessagePreview: String?,
    val unreadCount: Int,
)
