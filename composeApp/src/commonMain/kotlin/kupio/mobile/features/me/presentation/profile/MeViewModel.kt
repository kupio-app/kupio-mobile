package kupio.mobile.features.me.presentation.profile

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
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.me.domain.repository.MeRepository

class MeViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val sessionManager: AuthSessionManager,
    private val meRepository: MeRepository,
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
        viewModelScope.launch {
            sessionManager.sessionState.collect { sessionState ->
                val user = when (sessionState) {
                    is SessionState.SignedIn -> sessionState.user
                    is SessionState.NeedsUsername -> sessionState.user
                    else -> null
                }
                _state.update { it.copy(user = user) }
            }
        }
        loadStats()
    }

    fun onIntent(intent: MeIntent) {
        when (intent) {
            MeIntent.ThemeToggleClicked -> toggleThemeMode()
            MeIntent.MyListingsClicked -> emitEffect(MeEffect.NavigateToMyListings)
            MeIntent.ChatsClicked -> emitEffect(MeEffect.NavigateToChats)
            MeIntent.FavouritesClicked -> emitEffect(MeEffect.NavigateToFavourites)
            MeIntent.SettingsClicked -> emitEffect(MeEffect.NavigateToSettings)
            MeIntent.TopUpBalanceClicked,
            MeIntent.PaymentsHistoryClicked,
            MeIntent.PromotionsPackagesClicked,
            MeIntent.EditProfileClicked,
            MeIntent.ReportsDashboardClicked -> Unit
        }
    }

    private fun loadStats() {
        _state.update { it.copy(isLoadingStats = true) }
        viewModelScope.launch {
            runCatching { meRepository.getStats() }
                .onSuccess { stats ->
                    _state.update { it.copy(stats = stats, isLoadingStats = false) }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoadingStats = false) }
                }
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
