package kupio.mobile.features.payments.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.payments.domain.repository.PaymentsRepository

private const val PAGE_SIZE = 20

class PaymentsHistoryViewModel(
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentsHistoryState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<PaymentsHistoryEffect>(Channel.BUFFERED)
    val effects: Flow<PaymentsHistoryEffect> = effectChannel.receiveAsFlow()

    private var currentOffset = 0

    init {
        loadFirstPage()
    }

    fun onIntent(intent: PaymentsHistoryIntent) {
        when (intent) {
            PaymentsHistoryIntent.BackClicked -> viewModelScope.launch {
                effectChannel.send(PaymentsHistoryEffect.NavigateBack)
            }
            PaymentsHistoryIntent.Refresh -> refresh()
            PaymentsHistoryIntent.LoadMore -> loadMore()
            is PaymentsHistoryIntent.FilterSelected -> _state.update { it.copy(filter = intent.filter) }
        }
    }

    private fun loadFirstPage() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { paymentsRepository.getTransactions(limit = PAGE_SIZE, offset = 0) }
                .onSuccess { transactions ->
                    currentOffset = transactions.size
                    _state.update {
                        it.copy(
                            transactions = transactions,
                            isLoading = false,
                            hasMore = transactions.size >= PAGE_SIZE,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message) }
                }
        }
    }

    private fun loadMore() {
        if (_state.value.isLoadingMore || !_state.value.hasMore) return
        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            runCatching { paymentsRepository.getTransactions(limit = PAGE_SIZE, offset = currentOffset) }
                .onSuccess { newTransactions ->
                    currentOffset += newTransactions.size
                    _state.update { s ->
                        s.copy(
                            transactions = s.transactions + newTransactions,
                            isLoadingMore = false,
                            hasMore = newTransactions.size >= PAGE_SIZE,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private fun refresh() {
        currentOffset = 0
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, errorMessage = null, hasMore = true) }
            try {
                runCatching { paymentsRepository.getTransactions(limit = PAGE_SIZE, offset = 0) }
                    .onSuccess { transactions ->
                        currentOffset = transactions.size
                        _state.update {
                            it.copy(
                                transactions = transactions,
                                hasMore = transactions.size >= PAGE_SIZE,
                            )
                        }
                    }
                    .onFailure { t ->
                        if (t is CancellationException) throw t
                        _state.update { it.copy(errorMessage = t.message) }
                    }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }
}
