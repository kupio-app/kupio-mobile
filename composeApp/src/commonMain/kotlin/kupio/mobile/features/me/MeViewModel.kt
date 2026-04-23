package kupio.mobile.features.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode

class MeViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MeState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<MeEffect>(Channel.BUFFERED)
    val effects: Flow<MeEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun onIntent(intent: MeIntent) {
        when (intent) {
            MeIntent.ThemeToggleClicked -> toggleThemeMode()
            MeIntent.OpenSettingsClicked -> emitEffect(MeEffect.NavigateToSettings)
        }
    }

    private fun toggleThemeMode() {
        val nextMode = when (_state.value.themeMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }

        viewModelScope.launch {
            preferencesRepository.setThemeMode(nextMode)
        }
    }

    private fun emitEffect(effect: MeEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}

