package kupio.mobile.features.auth.data.repository

import io.ktor.client.request.HttpRequestBuilder
import kupio.mobile.core.network.ApiException
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.network.bearerAuth
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException

class TokenRefreshingAuthenticatedApiClient(
    private val authTokenProvider: AuthTokenProvider,
    private val onSessionExpired: suspend () -> Unit,
) : AuthenticatedApiClient {
    override suspend fun <T> request(
        block: suspend (authorize: HttpRequestBuilder.() -> Unit) -> T,
    ): T {
        return try {
            val token = authTokenProvider.freshAccessToken()
            try {
                block { bearerAuth(token) }
            } catch (throwable: Throwable) {
                if (throwable !is ApiException || throwable.statusCode != 401) throw throwable
                val refreshedToken = authTokenProvider.refreshAfterUnauthorized(token)
                try {
                    block { bearerAuth(refreshedToken) }
                } catch (retryThrowable: Throwable) {
                    if (retryThrowable is ApiException && retryThrowable.statusCode == 401) {
                        throw AuthSessionExpiredException()
                    }
                    throw retryThrowable
                }
            }
        } catch (expired: AuthSessionExpiredException) {
            onSessionExpired()
            throw expired
        }
    }
}
