package kupio.mobile.features.payments.presentation.topup

sealed interface TopUpError {
    data object Generic : TopUpError
}
