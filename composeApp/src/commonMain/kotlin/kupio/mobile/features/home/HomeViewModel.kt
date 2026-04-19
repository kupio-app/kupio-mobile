package kupio.mobile.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HomeState(
    val title: String = "Kupio Starter",
    val body: String = "This starter defines the shared app shell and demonstrates the initial MVI-lite structure.",
) : UiState

sealed interface HomeAction : UiAction {
    data object OpenSettingsClicked : HomeAction
}

sealed interface HomeEffect : UiEffect {
    data object NavigateToSettings : HomeEffect
}

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OpenSettingsClicked -> emitEffect(HomeEffect.NavigateToSettings)
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}
