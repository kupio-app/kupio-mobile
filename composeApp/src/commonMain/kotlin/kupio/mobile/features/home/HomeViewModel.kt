package kupio.mobile.features.home

import androidx.lifecycle.ViewModel
import kupio.mobile.core.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeState(
    val placeholder: Unit = Unit,
) : UiState

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()
}
