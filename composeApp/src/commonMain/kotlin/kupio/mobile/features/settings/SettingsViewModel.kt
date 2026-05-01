package kupio.mobile.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val selectedThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSigningOut: Boolean = false,
    val userId: String = "",
) : UiState

sealed interface SettingsAction : UiAction {
    data object NavigateBackClicked : SettingsAction
    data object LogoutClicked : SettingsAction
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsAction
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val sessionManager: AuthSessionManager,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
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
        viewModelScope.launch {
            sessionManager.sessionState.collect { session ->
                val id = (session as? SessionState.SignedIn)?.user?.id.orEmpty()
                _state.value = _state.value.copy(userId = id)
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.NavigateBackClicked -> emitEffect(SettingsEffect.NavigateBack)
            SettingsAction.LogoutClicked -> signOut()
            is SettingsAction.ThemeModeSelected -> updateThemeMode(action.mode)
        }
    }

    private fun emitEffect(effect: SettingsEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }

    private fun updateThemeMode(mode: ThemeMode) {
        if (_state.value.selectedThemeMode == mode || _state.value.isSigningOut) return
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
            analytics.logEvent("set_theme", mapOf("theme" to mode.name))
        }
    }

    private fun signOut() {
        if (_state.value.isSigningOut) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSigningOut = true)
            runCatching {
                sessionManager.signOut()
            }
            analytics.logEvent("logout")
            analytics.setUserId(null)
            _state.value = _state.value.copy(isSigningOut = false)
        }
    }
}
