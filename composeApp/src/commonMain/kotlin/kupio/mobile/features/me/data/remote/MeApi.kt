package kupio.mobile.features.me.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import kupio.mobile.core.network.bodyOrThrow
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.UserListingStats

class MeApi(private val httpClient: HttpClient) {

    suspend fun getStats(
        authorize: HttpRequestBuilder.() -> Unit,
    ): UserListingStats = httpClient.get("/api/users/me/stats") {
        authorize()
    }.bodyOrThrow<UserListingStatsDto>().toDomain()

    suspend fun getMyListings(
        authorize: HttpRequestBuilder.() -> Unit,
        limit: Int = 50,
        cursor: String? = null,
    ): List<OwnedListing> = httpClient.get("/api/users/me/listings") {
        authorize()
        url {
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow<ListOwnerListingsResponseDto>().listings.map { it.toDomain() }
}
