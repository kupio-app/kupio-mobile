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
import kupio.mobile.features.auth.domain.AuthSession
import kupio.mobile.features.auth.domain.DeviceIdProvider
import kupio.mobile.features.auth.domain.SecureSessionStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `refresh and me use stored tokens and bearer auth`() = kotlinx.coroutines.test.runTest {
        val secureSessionStore = FakeSecureSessionStore(
            session = AuthSession(
                accessToken = "stored-access",
                refreshToken = "stored-refresh",
                accessExpiresAt = 100,
                refreshExpiresAt = 200,
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
                              "access_expires_at":300,
                              "refresh_expires_at":400,
                              "needs_username":true
                            }
                            """.trimIndent(),
                        )
                    }

                    "/api/users/me" -> {
                        assertEquals(
                            "Bearer stored-access",
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
                accessExpiresAt = 100,
                refreshExpiresAt = 200,
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

        return AuthRepositoryImpl(
            authApi = AuthApi(httpClient),
            deviceIdProvider = object : DeviceIdProvider {
                override suspend fun getOrCreate(): String = "device-123"
            },
            secureSessionStore = secureSessionStore,
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
