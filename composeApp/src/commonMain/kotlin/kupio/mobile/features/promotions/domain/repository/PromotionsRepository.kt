package kupio.mobile.features.promotions.domain.repository

import kupio.mobile.features.promotions.domain.model.ListingPromotion
import kupio.mobile.features.promotions.domain.model.PromotionPacket

interface PromotionsRepository {
    suspend fun getPackets(): List<PromotionPacket>

    suspend fun promoteListing(
        listingId: String,
        packetId: Int,
    ): ListingPromotion
}
