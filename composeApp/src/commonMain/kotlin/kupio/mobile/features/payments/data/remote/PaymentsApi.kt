package kupio.mobile.features.payments.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.core.network.bodyOrThrow

@Serializable
data class CheckoutRequestDto(
    val amount: Int,
    @SerialName("success_url") val successUrl: String,
)

@Serializable
data class CheckoutResponseDto(
    @SerialName("checkout_url") val checkoutUrl: String,
    @SerialName("session_id") val sessionId: String,
)

@Serializable
data class BalanceTransactionDto(
    val id: String,
    val amount: Int,
    val type: String,
    @SerialName("created_at") val createdAt: String,
)

class PaymentsApi(private val httpClient: HttpClient) {

    suspend fun createCheckout(
        authorize: HttpRequestBuilder.() -> Unit,
        amountCents: Int,
        successUrl: String,
    ): CheckoutResponseDto = httpClient.post("/api/payments/checkout") {
        authorize()
        setBody(CheckoutRequestDto(amount = amountCents, successUrl = successUrl))
    }.bodyOrThrow()

    suspend fun getTransactions(
        authorize: HttpRequestBuilder.() -> Unit,
        limit: Int = 200,
        offset: Int = 0,
    ): List<BalanceTransactionDto> = httpClient.get("/api/payments/transactions") {
        authorize()
        url {
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }
    }.bodyOrThrow()
}
