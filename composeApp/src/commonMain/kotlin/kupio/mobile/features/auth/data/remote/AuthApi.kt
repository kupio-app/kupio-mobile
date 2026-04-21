package kupio.mobile.features.auth.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kupio.mobile.core.network.bodyOrThrow
import kupio.mobile.core.network.toApiException

class AuthApi(
    private val httpClient: HttpClient,
) {
    suspend fun login(
        request: LoginRequestDto,
    ): TokensResponseDto {
        return httpClient.post("/api/auth/login") {
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun register(
        request: RegisterRequestDto,
    ): TokensResponseDto {
        return httpClient.post("/api/auth/register") {
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun loginWithGoogle(
        request: GoogleLoginRequestDto,
    ): TokensResponseDto {
        return httpClient.post("/api/auth/google") {
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun refresh(
        request: RefreshRequestDto,
    ): TokensResponseDto {
        return httpClient.post("/api/auth/refresh") {
            setBody(request)
        }.bodyOrThrow()
    }

    suspend fun logout(
        request: LogoutRequestDto,
    ) {
        val response = httpClient.post("/api/auth/logout") {
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            throw response.toApiException()
        }
    }

    suspend fun getCurrentUser(
        accessToken: String,
    ): UserPrivateDto {
        return httpClient.get("/api/users/me") {
            bearerAuth(accessToken)
        }.bodyOrThrow()
    }

    suspend fun setUsername(
        accessToken: String,
        request: SetUsernameRequestDto,
    ): UserPrivateDto {
        return httpClient.patch("/api/users/me/username") {
            bearerAuth(accessToken)
            setBody(request)
        }.bodyOrThrow()
    }

    private fun HttpRequestBuilder.bearerAuth(
        accessToken: String,
    ) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}
