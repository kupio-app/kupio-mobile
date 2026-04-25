package kupio.mobile.features.chats.data

import kupio.mobile.features.chats.data.remote.MessageResponseDto
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.MessageSender

internal fun String.toTimeLabel(): String {
    return try {
        val tIndex = indexOf('T')
        if (tIndex < 0) return this
        val timePart = substring(tIndex + 1)
        val colonIndex = timePart.indexOf(':')
        if (colonIndex < 0) return this
        val secondColonIndex = timePart.indexOf(':', colonIndex + 1)
        if (secondColonIndex < 0) return this
        timePart.substring(0, secondColonIndex)
    } catch (_: Exception) {
        this
    }
}

internal fun MessageResponseDto.toItem(currentUserId: String) = MessageItem(
    id = id,
    sender = if (senderId == currentUserId) MessageSender.ME else MessageSender.THEM,
    text = content ?: "",
    timeLabel = createdAt.toTimeLabel(),
    isDeleted = isDeleted,
)
