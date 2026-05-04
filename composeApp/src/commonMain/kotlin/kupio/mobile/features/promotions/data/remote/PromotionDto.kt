package kupio.mobile.features.promotions.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.promotions.domain.model.ListingPromotion
import kupio.mobile.features.promotions.domain.model.PromotionPacket
import kupio.mobile.features.promotions.domain.model.PromotionStatus
import kupio.mobile.features.promotions.domain.model.PromotionType

@Serializable
data class PromotionPacketResponseDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val type: String,
    @SerialName("duration_days") val durationDays: Int,
    val price: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class PurchasePromotionRequestDto(
    @SerialName("packet_id") val packetId: Int,
)

@Serializable
data class ListingPromotionResponseDto(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    val packet: PromotionPacketResponseDto,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("expires_at") val expiresAt: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

fun PromotionPacketResponseDto.toDomain(): PromotionPacket = PromotionPacket(
    id = id,
    name = name,
    description = description,
    type = type.toPromotionType(),
    durationDays = durationDays,
    price = price,
    isActive = isActive,
)

fun ListingPromotionResponseDto.toDomain(): ListingPromotion = ListingPromotion(
    id = id,
    listingId = listingId,
    packet = packet.toDomain(),
    transactionId = transactionId,
    startsAt = startsAt,
    expiresAt = expiresAt,
    status = status.toPromotionStatus(),
    createdAt = createdAt,
)

private fun String.toPromotionType(): PromotionType = when (lowercase()) {
    "top" -> PromotionType.TOP
    "highlight" -> PromotionType.HIGHLIGHT
    "urgent" -> PromotionType.URGENT
    "vip" -> PromotionType.VIP
    else -> PromotionType.UNKNOWN
}

private fun String.toPromotionStatus(): PromotionStatus = when (lowercase()) {
    "pending" -> PromotionStatus.PENDING
    "active" -> PromotionStatus.ACTIVE
    "expired" -> PromotionStatus.EXPIRED
    "cancelled" -> PromotionStatus.CANCELLED
    else -> PromotionStatus.UNKNOWN
}
