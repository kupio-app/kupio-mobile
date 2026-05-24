package kupio.mobile.features.payments.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.payments.data.remote.BalanceTransactionDto
import kupio.mobile.features.payments.data.remote.PaymentsApi
import kupio.mobile.features.payments.domain.model.BalanceTransaction
import kupio.mobile.features.payments.domain.model.TransactionType
import kupio.mobile.features.payments.domain.repository.PaymentsRepository

class PaymentsRepositoryImpl(
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val paymentsApi: PaymentsApi,
) : PaymentsRepository {

    override suspend fun createCheckout(amountCents: Int): String =
        authenticatedApiClient.request { authorize ->
            val successUrl = "kupio://payment/success?amount=$amountCents"
            paymentsApi.createCheckout(authorize, amountCents, successUrl)
        }.checkoutUrl

    override suspend fun getTransactions(limit: Int, offset: Int): List<BalanceTransaction> =
        authenticatedApiClient.request { authorize ->
            paymentsApi.getTransactions(authorize, limit, offset)
        }.map { it.toDomain() }
}

private fun BalanceTransactionDto.toDomain() = BalanceTransaction(
    id = id,
    amountCents = amount,
    type = type.toTransactionType(),
    createdAt = createdAt,
)

private fun String.toTransactionType(): TransactionType = when (lowercase()) {
    "top_up" -> TransactionType.TOP_UP
    "refund" -> TransactionType.REFUND
    else -> TransactionType.DEBIT
}
