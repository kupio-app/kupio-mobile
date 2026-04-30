package kupio.mobile.core.notifications.data

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kupio.mobile.core.network.toApiException

class NotificationsApi(
    private val httpClient: HttpClient,
) {
    suspend fun sendPushToken(
        authorize: HttpRequestBuilder.() -> Unit,
        request: CreatePushTokenRequestDto,
    ) {
        val response = httpClient.post("/api/users/me/notification-tokens") {
            authorize()
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            throw response.toApiException()
        }
    }
}