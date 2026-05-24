package kupio.mobile.features.payments.presentation.topup

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState

data class TopUpState(
    val currentBalanceCents: Int = 0,
    val selectedAmountCents: Int = 2000,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface TopUpIntent : UiAction {
    data object BackClicked : TopUpIntent
    data class AmountSelected(val cents: Int) : TopUpIntent
    data object ContinueClicked : TopUpIntent
}

sealed interface TopUpEffect : UiEffect {
    data object NavigateBack : TopUpEffect
    data class OpenUrl(val url: String) : TopUpEffect
    data class ShowError(val message: String) : TopUpEffect
}
