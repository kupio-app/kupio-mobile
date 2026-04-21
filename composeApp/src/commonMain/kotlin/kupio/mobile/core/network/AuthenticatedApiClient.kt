package kupio.mobile.core.network

interface AuthenticatedApiClient {
    suspend fun <T> request(
        block: suspend (accessToken: String) -> T,
    ): T
}
