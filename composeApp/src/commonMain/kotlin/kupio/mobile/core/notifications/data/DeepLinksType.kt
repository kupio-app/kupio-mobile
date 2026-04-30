package kupio.mobile.core.notifications.data

enum class DeepLinksType(
    val rawString: String
) {
    ChatMessage("chat_message");

    companion object {
        fun from(value: String?): DeepLinksType? {
            return entries.firstOrNull { it.rawString == value }
        }
    }
}