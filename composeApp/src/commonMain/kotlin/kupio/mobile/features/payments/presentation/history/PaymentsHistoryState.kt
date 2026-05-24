package kupio.mobile.features.payments.presentation.history

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.payments.domain.model.BalanceTransaction

data class PaymentsHistoryState(
    val transactions: List<BalanceTransaction> = emptyList(),
    val filter: PaymentsFilter = PaymentsFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
) : UiState

enum class PaymentsFilter { ALL, TOP_UPS, SPENDING }

sealed interface PaymentsHistoryIntent : UiAction {
    data object BackClicked : PaymentsHistoryIntent
    data object Refresh : PaymentsHistoryIntent
    data object LoadMore : PaymentsHistoryIntent
    data class FilterSelected(val filter: PaymentsFilter) : PaymentsHistoryIntent
}

sealed interface PaymentsHistoryEffect : UiEffect {
    data object NavigateBack : PaymentsHistoryEffect
}
