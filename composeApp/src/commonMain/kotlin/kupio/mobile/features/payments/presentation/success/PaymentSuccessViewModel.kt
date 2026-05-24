package kupio.mobile.features.payments.presentation.success

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
import kupio.mobile.features.auth.domain.repository.AuthRepository
import kupio.mobile.features.auth.domain.session.AuthSessionManager

class PaymentSuccessViewModel(
    amountCents: Int,
    private val authRepository: AuthRepository,
    private val sessionManager: AuthSessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentSuccessState(amountCents = amountCents))
    val state = _state.asStateFlow()

    private val effectChannel = Channel<PaymentSuccessEffect>(Channel.BUFFERED)
    val effects: Flow<PaymentSuccessEffect> = effectChannel.receiveAsFlow()

    init {
        refreshBalance()
    }

    fun onIntent(intent: PaymentSuccessIntent) {
        when (intent) {
            PaymentSuccessIntent.DoneClicked -> viewModelScope.launch {
                effectChannel.send(PaymentSuccessEffect.GoToHistory)
            }
        }
    }

    private fun refreshBalance() {
        viewModelScope.launch {
            runCatching { authRepository.getCurrentUser() }
                .onSuccess { user ->
                    sessionManager.updateAuthenticatedUser(user)
                    _state.update { it.copy(newBalanceCents = user.balance) }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                }
        }
    }
}
