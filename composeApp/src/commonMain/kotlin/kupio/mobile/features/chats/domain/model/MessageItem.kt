package kupio.mobile.features.chats.domain.model

enum class MessageSender { ME, THEM }

data class MessageItem(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timeLabel: String,
    val isDeleted: Boolean = false,
)
