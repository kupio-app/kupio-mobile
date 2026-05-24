package kupio.mobile.features.payments.presentation.success

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState

data class PaymentSuccessState(
    val amountCents: Int,
    val newBalanceCents: Int? = null,
) : UiState

sealed interface PaymentSuccessIntent : UiAction {
    data object DoneClicked : PaymentSuccessIntent
}

sealed interface PaymentSuccessEffect : UiEffect {
    data object GoToHistory : PaymentSuccessEffect
}
