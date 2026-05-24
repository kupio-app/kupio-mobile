package kupio.mobile.features.payments.domain.repository

import kupio.mobile.features.payments.domain.model.BalanceTransaction

interface PaymentsRepository {
    suspend fun createCheckout(amountCents: Int): String
    suspend fun getTransactions(limit: Int = 20, offset: Int = 0): List<BalanceTransaction>
}
