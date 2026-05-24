package kupio.mobile.features.payments.presentation.topup

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

class TopUpViewModel(
    private val currentBalanceCents: Int,
    private val paymentsRepository: PaymentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TopUpState(currentBalanceCents = currentBalanceCents))
    val state = _state.asStateFlow()

    private val effectChannel = Channel<TopUpEffect>(Channel.BUFFERED)
    val effects: Flow<TopUpEffect> = effectChannel.receiveAsFlow()

    fun onIntent(intent: TopUpIntent) {
        when (intent) {
            TopUpIntent.BackClicked -> emitEffect(TopUpEffect.NavigateBack)
            is TopUpIntent.AmountSelected -> _state.update { it.copy(selectedAmountCents = intent.cents) }
            TopUpIntent.ContinueClicked -> checkout()
        }
    }

    private fun checkout() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                paymentsRepository.createCheckout(_state.value.selectedAmountCents)
            }.onSuccess { url ->
                _state.update { it.copy(isLoading = false) }
                effectChannel.send(TopUpEffect.OpenUrl(url))
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update { it.copy(isLoading = false, error = TopUpError.Generic) }
            }
        }
    }

    private fun emitEffect(effect: TopUpEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
