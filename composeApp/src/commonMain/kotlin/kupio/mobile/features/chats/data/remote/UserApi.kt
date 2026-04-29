package kupio.mobile.features.chats.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.core.network.bodyOrThrow

@Serializable
data class UserPublicDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
)

class UserApi(private val httpClient: HttpClient) {

    suspend fun getUserByUsername(
        authorize: HttpRequestBuilder.() -> Unit,
        username: String,
    ): UserPublicDto = httpClient.get("/api/users/$username") {
        authorize()
    }.bodyOrThrow()

    suspend fun getUserById(
        authorize: HttpRequestBuilder.() -> Unit,
        userId: String,
    ): UserPublicDto = httpClient.get("/api/users/id/$userId") {
        authorize()
    }.bodyOrThrow()
}
