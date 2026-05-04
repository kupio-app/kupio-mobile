package kupio.mobile.features.promotions.domain.model

data class PromotionPacket(
    val id: Int,
    val name: String,
    val description: String?,
    val type: PromotionType,
    val durationDays: Int,
    val price: Int,
    val isActive: Boolean,
)

enum class PromotionType {
    TOP,
    HIGHLIGHT,
    URGENT,
    VIP,
    UNKNOWN,
}

data class ListingPromotion(
    val id: String,
    val listingId: String,
    val packet: PromotionPacket,
    val transactionId: String?,
    val startsAt: String,
    val expiresAt: String,
    val status: PromotionStatus,
    val createdAt: String,
)

enum class PromotionStatus {
    PENDING,
    ACTIVE,
    EXPIRED,
    CANCELLED,
    UNKNOWN,
}
