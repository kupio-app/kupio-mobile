package kupio.mobile.features.chats.data

import kupio.mobile.core.datetime.toTimeLabel
import kupio.mobile.features.chats.data.remote.MessageResponseDto
import kupio.mobile.features.chats.domain.model.ListingSummary
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.MessageSender
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice

internal fun MessageResponseDto.toItem(currentUserId: String) = MessageItem(
    id = id,
    sender = if (senderId == currentUserId) MessageSender.ME else MessageSender.THEM,
    text = content ?: "",
    timeLabel = createdAt.toTimeLabel(),
    createdAtIso = createdAt,
    isDeleted = isDeleted,
)

internal fun Listing.toSummary() = ListingSummary(
    id = id,
    title = title,
    priceFormatted = formatPrice(),
    placeholderSeed = id.hashCode(),
    imageUrl = primaryImageUrl,
)
