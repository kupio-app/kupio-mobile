package kupio.mobile.features.home.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kupio.mobile.core.network.bodyOrThrow

class ListingsApi(private val httpClient: HttpClient) {
    suspend fun getListings(
        query: String? = null,
        categoryId: Int? = null,
        limit: Int = 20,
        cursor: String? = null,
    ): ListListingsResponseDto = httpClient.get("/api/listings") {
        url {
            query?.let { parameters.append("q", it) }
            categoryId?.let { parameters.append("category_id", it.toString()) }
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow()
}
