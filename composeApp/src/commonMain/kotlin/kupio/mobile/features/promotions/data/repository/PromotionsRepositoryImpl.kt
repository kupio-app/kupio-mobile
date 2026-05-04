package kupio.mobile.features.promotions.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.promotions.data.remote.PromotionsApi
import kupio.mobile.features.promotions.data.remote.PurchasePromotionRequestDto
import kupio.mobile.features.promotions.data.remote.toDomain
import kupio.mobile.features.promotions.domain.model.ListingPromotion
import kupio.mobile.features.promotions.domain.model.PromotionPacket
import kupio.mobile.features.promotions.domain.repository.PromotionsRepository

class PromotionsRepositoryImpl(
    private val promotionsApi: PromotionsApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : PromotionsRepository {

    override suspend fun getPackets(): List<PromotionPacket> =
        promotionsApi.getPackets().map { it.toDomain() }

    override suspend fun promoteListing(
        listingId: String,
        packetId: Int,
    ): ListingPromotion = authenticatedApiClient.request { authorize ->
        promotionsApi.promoteListing(
            authorize = authorize,
            listingId = listingId,
            request = PurchasePromotionRequestDto(packetId),
        ).toDomain()
    }
}
