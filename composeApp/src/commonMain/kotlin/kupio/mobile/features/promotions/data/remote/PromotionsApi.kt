package kupio.mobile.features.promotions.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kupio.mobile.core.network.bodyOrThrow

class PromotionsApi(private val httpClient: HttpClient) {

    suspend fun getPackets(): List<PromotionPacketResponseDto> =
        httpClient.get("/api/promotions/packets").bodyOrThrow()

    suspend fun promoteListing(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        request: PurchasePromotionRequestDto,
    ): ListingPromotionResponseDto = httpClient.post("/api/listings/promotions/$listingId") {
        authorize()
        setBody(request)
    }.bodyOrThrow()
}
