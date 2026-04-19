package kupio.mobile.features.settings

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

data class SettingsState(
    val title: String = "Settings",
    val body: String = "Theme persistence and localization are wired in the next implementation steps.",
) : UiState

sealed interface SettingsAction : UiAction {
    data object NavigateBackClicked : SettingsAction
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
}

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = effectChannel.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.NavigateBackClicked -> emitEffect(SettingsEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: SettingsEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}
