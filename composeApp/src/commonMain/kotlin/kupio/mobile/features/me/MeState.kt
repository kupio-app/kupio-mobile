package kupio.mobile.features.me

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.core.preferences.ThemeMode

data class MeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) : UiState

sealed interface MeIntent : UiAction {
    data object ThemeToggleClicked : MeIntent
    data object OpenSettingsClicked : MeIntent
}

sealed interface MeEffect : UiEffect {
    data object NavigateToSettings : MeEffect
}

