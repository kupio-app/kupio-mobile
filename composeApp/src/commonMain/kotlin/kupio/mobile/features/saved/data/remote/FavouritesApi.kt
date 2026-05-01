package kupio.mobile.features.saved.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.core.network.bodyOrThrow
import kupio.mobile.features.listings.data.remote.ListListingsResponseDto

@Serializable
data class FavouriteIdsResponseDto(
    @SerialName("listings_ids") val listingsIds: List<String>,
)

class FavouritesApi(private val httpClient: HttpClient) {

    suspend fun getFavourites(
        authorize: HttpRequestBuilder.() -> Unit,
        limit: Int = 20,
        cursor: String? = null,
    ): ListListingsResponseDto = httpClient.get("/api/listings/favourites") {
        authorize()
        url {
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow()

    suspend fun getFavouriteIds(
        authorize: HttpRequestBuilder.() -> Unit,
    ): FavouriteIdsResponseDto = httpClient.get("/api/listings/favourites/ids") {
        authorize()
    }.bodyOrThrow()

    suspend fun addFavourite(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
    ) {
        httpClient.post("/api/listings/favourites/$listingId") {
            authorize()
        }
    }

    suspend fun removeFavourite(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
    ) {
        httpClient.delete("/api/listings/favourites/$listingId") {
            authorize()
        }
    }
}
