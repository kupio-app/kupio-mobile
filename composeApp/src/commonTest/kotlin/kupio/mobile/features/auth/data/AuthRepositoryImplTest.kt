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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kupio.mobile.features.auth.data.remote.AuthApi
import kupio.mobile.features.auth.data.repository.AuthClock
import kupio.mobile.features.auth.data.repository.AuthRepositoryImpl
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.data.repository.TokenRefreshingAuthenticatedApiClient
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.repository.DeviceIdProvider
import kupio.mobile.features.auth.domain.session.SecureSessionStore

class AuthRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `login sends device id and maps tokens response`() = kotlinx.coroutines.test.runTest {
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                assertEquals("/api/auth/login", request.url.encodedPath)
                val body = request.bodyText()
                assertTrue(body.contains("\"device_id\":\"device-123\""))
                assertTrue(body.contains("\"email\":\"hello@kupio.dev\""))
                respondJson(
                    """
                    {
                      "access_token":"access",
                      "refresh_token":"refresh",
                      "access_expires_at":100,
                      "refresh_expires_at":200,
                      "token_type":"bearer",
                      "needs_username":false
                    }
                    """.trimIndent(),
                )
            },
        )

        val session = repository.login(
            email = "hello@kupio.dev",
            password = "password123",
        )

        assertEquals(
            AuthSession(
                accessToken = "access",
                refreshToken = "refresh",
                accessExpiresAt = 100,
                refreshExpiresAt = 200,
                tokenType = "bearer",
                needsUsername = false,
            ),
            session,
        )
    }

    @Test
    fun `refresh stores rotated tokens and me uses fresh bearer auth`() = kotlinx.coroutines.test.runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "stored-access",
                refreshToken = "stored-refresh",
                accessExpiresAt = 2_000,
                refreshExpiresAt = 3_000,
            ),
        )
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/auth/refresh" -> {
                        val body = request.bodyText()
                        assertTrue(body.contains("\"refresh_token\":\"stored-refresh\""))
                        respondJson(
                            """
                            {
                              "access_token":"fresh-access",
                              "refresh_token":"fresh-refresh",
                              "access_expires_at":2000,
                              "refresh_expires_at":4000,
                              "needs_username":true
                            }
                            """.trimIndent(),
                        )
                    }

                    "/api/users/me" -> {
                        assertEquals(
                            "Bearer fresh-access",
                            request.headers[HttpHeaders.Authorization],
                        )
                        respondJson(
                            """
                            {
                              "id":"user-1",
                              "username":null,
                              "display_name":"Kupio User",
                              "email":"hello@kupio.dev",
                              "role":"user",
                              "needs_username":true,
                              "balance":0,
                              "avatar_url":null
                            }
                            """.trimIndent(),
                        )
                    }

                    else -> error("Unexpected path ${request.url.encodedPath}")
                }
            },
            secureSessionStore = secureSessionStore,
        )

        val refreshed = repository.refreshSession()
        val user = repository.getCurrentUser()

        assertEquals("fresh-access", refreshed.accessToken)
        assertEquals("fresh-access", secureSessionStore.readSession()?.accessToken)
        assertEquals(true, refreshed.needsUsername)
        assertEquals("hello@kupio.dev", user.email)
        assertEquals(true, user.needsUsername)
    }

    @Test
    fun `set username sends bearer auth and request body`() = kotlinx.coroutines.test.runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "stored-access",
                refreshToken = "stored-refresh",
                accessExpiresAt = 2_000,
                refreshExpiresAt = 3_000,
            ),
        )
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                assertEquals("/api/users/me/username", request.url.encodedPath)
                assertEquals("Bearer stored-access", request.headers[HttpHeaders.Authorization])
                assertTrue(request.bodyText().contains("\"username\":\"kupio\""))
                respondJson(
                    """
                    {
                      "id":"user-1",
                      "username":"kupio",
                      "display_name":"Kupio User",
                      "email":"hello@kupio.dev",
                      "role":"user",
                      "needs_username":false,
                      "balance":0,
                      "avatar_url":null
                    }
                    """.trimIndent(),
                )
            },
            secureSessionStore = secureSessionStore,
        )

        val user = repository.setUsername("kupio")

        assertEquals("kupio", user.username)
        assertEquals(false, user.needsUsername)
    }

    @Test
    fun `expired access token refreshes before current user request`() = kotlinx.coroutines.test.runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "expired-access",
                refreshToken = "stored-refresh",
                accessExpiresAt = 900,
                refreshExpiresAt = 3_000,
            ),
        )
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/auth/refresh" -> {
                        assertTrue(request.bodyText().contains("\"refresh_token\":\"stored-refresh\""))
                        respondTokenPair(
                            accessToken = "fresh-access",
                            refreshToken = "fresh-refresh",
                            accessExpiresAt = 2_000,
                            refreshExpiresAt = 4_000,
                        )
                    }

                    "/api/users/me" -> {
                        assertEquals("Bearer fresh-access", request.headers[HttpHeaders.Authorization])
                        respondUser()
                    }

                    else -> error("Unexpected path ${request.url.encodedPath}")
                }
            },
            secureSessionStore = secureSessionStore,
        )

        val user = repository.getCurrentUser()

        assertEquals("hello@kupio.dev", user.email)
        assertEquals("fresh-access", secureSessionStore.readSession()?.accessToken)
    }

    @Test
    fun `current user retries once after unauthorized access token`() = kotlinx.coroutines.test.runTest {
        var meCalls = 0
        var refreshCalls = 0
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/users/me" -> {
                        meCalls += 1
                        if (meCalls == 1) {
                            assertEquals("Bearer stored-access", request.headers[HttpHeaders.Authorization])
                            respondJson("""{"detail":"Invalid token"}""", status = HttpStatusCode.Unauthorized)
                        } else {
                            assertEquals("Bearer fresh-access", request.headers[HttpHeaders.Authorization])
                            respondUser()
                        }
                    }

                    "/api/auth/refresh" -> {
                        refreshCalls += 1
                        respondTokenPair(
                            accessToken = "fresh-access",
                            refreshToken = "fresh-refresh",
                            accessExpiresAt = 2_000,
                            refreshExpiresAt = 4_000,
                        )
                    }

                    else -> error("Unexpected path ${request.url.encodedPath}")
                }
            },
            secureSessionStore = FakeSecureSessionStore(
                session = AuthSession(
                    accessToken = "stored-access",
                    refreshToken = "stored-refresh",
                    accessExpiresAt = 2_000,
                    refreshExpiresAt = 3_000,
                ),
            ),
        )

        val user = repository.getCurrentUser()

        assertEquals("hello@kupio.dev", user.email)
        assertEquals(2, meCalls)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `current user retry unauthorized clears session and fails`() = kotlinx.coroutines.test.runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "stored-access",
                refreshToken = "stored-refresh",
                accessExpiresAt = 2_000,
                refreshExpiresAt = 3_000,
            ),
        )
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/users/me" -> respondJson(
                        """{"detail":"Invalid token"}""",
                        status = HttpStatusCode.Unauthorized,
                    )

                    "/api/auth/refresh" -> respondTokenPair(
                        accessToken = "fresh-access",
                        refreshToken = "fresh-refresh",
                        accessExpiresAt = 2_000,
                        refreshExpiresAt = 4_000,
                    )

                    else -> error("Unexpected path ${request.url.encodedPath}")
                }
            },
            secureSessionStore = secureSessionStore,
        )

        assertFailsWith<AuthSessionExpiredException> {
            repository.getCurrentUser()
        }
        assertNull(secureSessionStore.readSession())
    }

    @Test
    fun `set username refreshes expired token and retries request`() = kotlinx.coroutines.test.runTest {
        var usernameCalls = 0
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/api/auth/refresh" -> respondTokenPair(
                        accessToken = "fresh-access",
                        refreshToken = "fresh-refresh",
                        accessExpiresAt = 2_000,
                        refreshExpiresAt = 4_000,
                    )

                    "/api/users/me/username" -> {
                        usernameCalls += 1
                        assertEquals("Bearer fresh-access", request.headers[HttpHeaders.Authorization])
                        assertTrue(request.bodyText().contains("\"username\":\"kupio\""))
                        respondUser(username = "kupio", needsUsername = false)
                    }

                    else -> error("Unexpected path ${request.url.encodedPath}")
                }
            },
            secureSessionStore = FakeSecureSessionStore(
                session = AuthSession(
                    accessToken = "expired-access",
                    refreshToken = "stored-refresh",
                    accessExpiresAt = 900,
                    refreshExpiresAt = 3_000,
                ),
            ),
        )

        val user = repository.setUsername("kupio")

        assertEquals("kupio", user.username)
        assertEquals(1, usernameCalls)
    }

    @Test
    fun `logout sends provided refresh token`() = kotlinx.coroutines.test.runTest {
        val repository = createRepository(
            mockEngine = MockEngine { request ->
                assertEquals("/api/auth/logout", request.url.encodedPath)
                assertTrue(request.bodyText().contains("\"refresh_token\":\"stored-refresh\""))
                respondJson("{}", status = HttpStatusCode.Accepted)
            },
        )

        repository.logout(refreshToken = "stored-refresh")
    }

    private fun createRepository(
        mockEngine: MockEngine,
        secureSessionStore: FakeSecureSessionStore = FakeSecureSessionStore(),
        clock: AuthClock = FakeAuthClock(nowEpochSeconds = 1_000),
    ): AuthRepositoryImpl {
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

        val authApi = AuthApi(httpClient)
        val deviceIdProvider = object : DeviceIdProvider {
            override suspend fun getOrCreate(): String = "device-123"
        }

        val authTokenProvider = AuthTokenProvider(
            authApi = authApi,
            deviceIdProvider = deviceIdProvider,
            secureSessionStore = secureSessionStore,
            clock = clock,
        )

        return AuthRepositoryImpl(
            authApi = authApi,
            deviceIdProvider = deviceIdProvider,
            authTokenProvider = authTokenProvider,
            authenticatedApiClient = TokenRefreshingAuthenticatedApiClient(authTokenProvider),
        )
    }

    private fun HttpRequestData.bodyText(): String {
        val body = body
        return when (body) {
            is io.ktor.http.content.TextContent -> body.text
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
            else -> error("Unsupported request body type ${body::class}")
        }
    }

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

    private fun MockRequestHandleScope.respondUser(
        username: String? = null,
        needsUsername: Boolean = true,
    ) = respondJson(
        """
        {
          "id":"user-1",
          "username":${username?.let { "\"$it\"" } ?: "null"},
          "display_name":"Kupio User",
          "email":"hello@kupio.dev",
          "role":"user",
          "needs_username":$needsUsername,
          "balance":0,
          "avatar_url":null
        }
        """.trimIndent(),
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
