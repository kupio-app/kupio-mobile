package kupio.mobile.features.me.presentation.mylistings

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
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.repository.MeRepository

class MyListingsViewModel(
    private val meRepository: MeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MyListingsState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<MyListingsEffect>(Channel.BUFFERED)
    val effects: Flow<MyListingsEffect> = effectChannel.receiveAsFlow()

    init {
        loadListings()
    }

    fun onIntent(intent: MyListingsIntent) {
        when (intent) {
            is MyListingsIntent.FilterSelected -> _state.update { it.copy(filter = intent.filter) }
            MyListingsIntent.BackClicked -> viewModelScope.launch {
                effectChannel.send(MyListingsEffect.NavigateBack)
            }
            is MyListingsIntent.EditListing -> Unit
            is MyListingsIntent.BumpUp -> Unit
            is MyListingsIntent.Promote -> Unit
            is MyListingsIntent.ToggleActive -> Unit
        }
    }

    private fun loadListings() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { meRepository.getMyListings() }
                .onSuccess { listings ->
                    val active = listings.count { it.status == OwnedListingStatus.ACTIVE }
                    val inactive = listings.count { it.status == OwnedListingStatus.INACTIVE }
                    _state.update {
                        it.copy(
                            listings = listings,
                            activeCount = active,
                            inactiveCount = inactive,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message) }
                }
        }
    }
}
