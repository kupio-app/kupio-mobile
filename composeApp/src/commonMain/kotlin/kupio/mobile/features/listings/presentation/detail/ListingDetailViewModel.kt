package kupio.mobile.features.listings.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ListingDetailViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    private val effectChannel = Channel<ListingDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ListingDetailEffect> = effectChannel.receiveAsFlow()

    init { load() }

    fun onIntent(intent: ListingDetailIntent) {
        when (intent) {
            ListingDetailIntent.Retry -> load()
            ListingDetailIntent.Back -> viewModelScope.launch {
                effectChannel.send(ListingDetailEffect.NavigateBack)
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { listingsRepository.getListing(listingId) }
                .onSuccess { listing ->
                    _state.update { it.copy(listing = listing, isLoading = false) }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message.orEmpty()) }
                }
        }
    }
}
