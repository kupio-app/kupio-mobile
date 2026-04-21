package kupio.mobile.core.network

import io.ktor.client.request.HttpRequestBuilder

interface AuthenticatedApiClient {
    suspend fun <T> request(
        block: suspend (authorize: HttpRequestBuilder.() -> Unit) -> T,
    ): T
}
