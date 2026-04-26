package kupio.mobile.features.chats.domain.model

data class ChatSummary(
    val id: String,
    val role: ChatRole,
    val participantId: String,
    val participantLabel: String,
    val participantInitials: String,
    val listing: ListingSummary,
    val lastMessagePreview: String?,
    val lastMessageTimeLabel: String,
    val unreadCount: Int,
)
