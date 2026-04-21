package kupio.mobile.features.auth.data.repository

import kupio.mobile.core.network.ApiException
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException

class TokenRefreshingAuthenticatedApiClient(
    private val authTokenProvider: AuthTokenProvider,
) : AuthenticatedApiClient {
    override suspend fun <T> request(
        block: suspend (accessToken: String) -> T,
    ): T {
        val accessToken = authTokenProvider.freshAccessToken()
        return try {
            block(accessToken)
        } catch (throwable: Throwable) {
            if (throwable !is ApiException || throwable.statusCode != 401) {
                throw throwable
            }

            val refreshedAccessToken = authTokenProvider.refreshAfterUnauthorized(accessToken)
            try {
                block(refreshedAccessToken)
            } catch (retryThrowable: Throwable) {
                if (retryThrowable is ApiException && retryThrowable.statusCode == 401) {
                    authTokenProvider.clearSession()
                    throw AuthSessionExpiredException()
                }
                throw retryThrowable
            }
        }
    }
}
