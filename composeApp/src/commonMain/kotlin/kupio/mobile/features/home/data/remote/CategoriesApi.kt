package kupio.mobile.features.home.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kupio.mobile.core.network.bodyOrThrow

class CategoriesApi(private val httpClient: HttpClient) {
    suspend fun getCategories(
        depth: Int? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): List<CategoryResponseDto> = httpClient.get("/api/categories") {
        url {
            depth?.let { parameters.append("depth", it.toString()) }
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }
    }.bodyOrThrow()
}
