package kupio.mobile.features.chats.domain.model

data class ConversationsPage(
    val conversations: List<ConversationData>,
    val nextCursor: String?,
)
