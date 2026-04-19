package kupio.mobile.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
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
    val selectedThemeMode: ThemeMode = ThemeMode.SYSTEM,
) : UiState

sealed interface SettingsAction : UiAction {
    data object NavigateBackClicked : SettingsAction
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsAction
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { themeMode ->
                _state.value = _state.value.copy(selectedThemeMode = themeMode)
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.NavigateBackClicked -> emitEffect(SettingsEffect.NavigateBack)
            is SettingsAction.ThemeModeSelected -> updateThemeMode(action.mode)
        }
    }

    private fun emitEffect(effect: SettingsEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }

    private fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }
}
