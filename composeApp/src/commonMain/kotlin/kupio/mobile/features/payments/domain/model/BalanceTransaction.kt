package kupio.mobile.features.payments.domain.model

data class BalanceTransaction(
    val id: String,
    val amountCents: Int,
    val type: TransactionType,
    val createdAt: String,
)

enum class TransactionType {
    TOP_UP, DEBIT, REFUND
}
