package kupio.mobile.debug

import io.ktor.client.request.HttpRequestBuilder
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.network.bearerAuth
import kupio.mobile.features.auth.domain.session.SecureSessionStore

internal class TestAuthenticatedApiClient(
    private val delegate: AuthenticatedApiClient,
    private val secureSessionStore: SecureSessionStore,
) : AuthenticatedApiClient {
    override suspend fun <T> request(
        block: suspend (authorize: HttpRequestBuilder.() -> Unit) -> T,
    ): T {
        val session = secureSessionStore.readSession()
        return if (
            session?.accessToken == TestAuthContract.AccessToken &&
            session.refreshToken == TestAuthContract.RefreshToken
        ) {
            block { bearerAuth(TestAuthContract.AccessToken) }
        } else {
            delegate.request(block)
        }
    }
}
