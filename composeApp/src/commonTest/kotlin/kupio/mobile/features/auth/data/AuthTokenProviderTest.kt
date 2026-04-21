package kupio.mobile.features.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.repository.AuthClock
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider
import kupio.mobile.features.auth.domain.session.SecureSessionStore

class AuthTokenProviderTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `fresh access token returns stored token without refresh`() = runTest {
        var refreshCalls = 0
        val provider = createProvider(
            mockEngine = MockEngine {
                refreshCalls += 1
                error("Refresh should not be called for a fresh access token")
            },
            secureSessionStore = FakeSecureSessionStore(
                session = sampleSession(
                    accessToken = "stored-access",
                    accessExpiresAt = 2_000,
                    refreshExpiresAt = 3_000,
                ),
            ),
        )

        assertEquals("stored-access", provider.freshAccessToken())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `near expired access token refreshes and stores rotated session`() = runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = sampleSession(
                accessToken = "old-access",
                refreshToken = "old-refresh",
                accessExpiresAt = 1_050,
                refreshExpiresAt = 3_000,
            ),
        )
        val provider = createProvider(
            mockEngine = MockEngine { request ->
                assertEquals("/api/auth/refresh", request.url.encodedPath)
                assertTrue(request.bodyText().contains("\"refresh_token\":\"old-refresh\""))
                assertTrue(request.bodyText().contains("\"device_id\":\"device-123\""))
                respondTokenPair(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    accessExpiresAt = 2_000,
                    refreshExpiresAt = 4_000,
                )
            },
            secureSessionStore = secureSessionStore,
        )

        assertEquals("new-access", provider.freshAccessToken())
        assertEquals(
            sampleSession(
                accessToken = "new-access",
                refreshToken = "new-refresh",
                accessExpiresAt = 2_000,
                refreshExpiresAt = 4_000,
            ),
            secureSessionStore.readSession(),
        )
    }

    @Test
    fun `expired refresh token clears session and fails`() = runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = sampleSession(
                accessExpiresAt = 900,
                refreshExpiresAt = 1_000,
            ),
        )
        val provider = createProvider(
            mockEngine = MockEngine {
                error("Refresh should not be called when refresh token is expired locally")
            },
            secureSessionStore = secureSessionStore,
            clock = FakeAuthClock(nowEpochSeconds = 1_000),
        )

        assertFailsWith<AuthSessionExpiredException> {
            provider.freshAccessToken()
        }
        assertNull(secureSessionStore.readSession())
    }

    @Test
    fun `refresh unauthorized clears session and fails`() = runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = sampleSession(
                accessExpiresAt = 900,
                refreshExpiresAt = 3_000,
            ),
        )
        val provider = createProvider(
            mockEngine = MockEngine {
                respondJson(
                    """{"detail":"Invalid or expired refresh token"}""",
                    status = HttpStatusCode.Unauthorized,
                )
            },
            secureSessionStore = secureSessionStore,
        )

        assertFailsWith<AuthSessionExpiredException> {
            provider.freshAccessToken()
        }
        assertNull(secureSessionStore.readSession())
    }

    @Test
    fun `transient refresh failure preserves session`() = runTest {
        val storedSession = sampleSession(
            accessExpiresAt = 900,
            refreshExpiresAt = 3_000,
        )
        val secureSessionStore = FakeSecureSessionStore(session = storedSession)
        val provider = createProvider(
            mockEngine = MockEngine {
                throw IllegalStateException("network unavailable")
            },
            secureSessionStore = secureSessionStore,
        )

        assertFailsWith<IllegalStateException> {
            provider.freshAccessToken()
        }
        assertEquals(storedSession, secureSessionStore.readSession())
    }

    @Test
    fun `concurrent expired access requests refresh once`() = runTest {
        var refreshCalls = 0
        val secureSessionStore = FakeSecureSessionStore(
            session = sampleSession(
                accessToken = "old-access",
                refreshToken = "old-refresh",
                accessExpiresAt = 900,
                refreshExpiresAt = 3_000,
            ),
        )
        val provider = createProvider(
            mockEngine = MockEngine {
                refreshCalls += 1
                respondTokenPair(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    accessExpiresAt = 2_000,
                    refreshExpiresAt = 4_000,
                )
            },
            secureSessionStore = secureSessionStore,
        )

        val tokens = List(5) {
            async { provider.freshAccessToken() }
        }.awaitAll()

        assertEquals(List(5) { "new-access" }, tokens)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `forced refresh reuses token already rotated by another caller`() = runTest {
        val provider = createProvider(
            mockEngine = MockEngine {
                error("Refresh should not be called when stored token already changed")
            },
            secureSessionStore = FakeSecureSessionStore(
                session = sampleSession(
                    accessToken = "new-access",
                    refreshToken = "new-refresh",
                    accessExpiresAt = 2_000,
                    refreshExpiresAt = 4_000,
                ),
            ),
        )

        assertEquals(
            "new-access",
            provider.refreshAfterUnauthorized(failedAccessToken = "old-access"),
        )
    }

    private fun createProvider(
        mockEngine: MockEngine,
        secureSessionStore: FakeSecureSessionStore,
        clock: AuthClock = FakeAuthClock(nowEpochSeconds = 1_000),
    ): AuthTokenProvider {
        val httpClient = HttpClient(mockEngine) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url("http://localhost:8000")
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
        }

        return AuthTokenProvider(
            authApi = AuthApi(httpClient),
            deviceIdProvider = object : DeviceIdProvider {
                override suspend fun getOrCreate(): String = "device-123"
            },
            secureSessionStore = secureSessionStore,
            clock = clock,
        )
    }

    private fun sampleSession(
        accessToken: String = "access",
        refreshToken: String = "refresh",
        accessExpiresAt: Long = 2_000,
        refreshExpiresAt: Long = 3_000,
    ) = AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessExpiresAt = accessExpiresAt,
        refreshExpiresAt = refreshExpiresAt,
    )

    private fun HttpRequestData.bodyText(): String {
        val body = body
        return when (body) {
            is io.ktor.http.content.TextContent -> body.text
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
            else -> error("Unsupported request body type ${body::class}")
        }
    }

    private fun MockRequestHandleScope.respondTokenPair(
        accessToken: String,
        refreshToken: String,
        accessExpiresAt: Long,
        refreshExpiresAt: Long,
    ) = respondJson(
        """
        {
          "access_token":"$accessToken",
          "refresh_token":"$refreshToken",
          "access_expires_at":$accessExpiresAt,
          "refresh_expires_at":$refreshExpiresAt,
          "needs_username":false
        }
        """.trimIndent(),
    )

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = ByteReadChannel(body),
        status = status,
        headers = headersOf(
            HttpHeaders.ContentType,
            ContentType.Application.Json.toString(),
        ),
    )

    private class FakeAuthClock(
        private val nowEpochSeconds: Long,
    ) : AuthClock {
        override fun nowEpochSeconds(): Long = nowEpochSeconds
    }

    private class FakeSecureSessionStore(
        private var session: AuthSession? = null,
    ) : SecureSessionStore {
        override suspend fun readSession(): AuthSession? = session

        override suspend fun writeSession(session: AuthSession) {
            this.session = session
        }

        override suspend fun clear() {
            session = null
        }
    }
}
