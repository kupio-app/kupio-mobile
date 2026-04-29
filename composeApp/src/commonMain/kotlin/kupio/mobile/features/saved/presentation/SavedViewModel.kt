package kupio.mobile.features.saved.presentation

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
import kupio.mobile.features.saved.domain.repository.FavouritesRepository

class SavedViewModel(
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedState())
    val state: StateFlow<SavedState> = _state.asStateFlow()

    private val effectChannel = Channel<SavedEffect>(Channel.BUFFERED)
    val effects: Flow<SavedEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: SavedIntent) {
        when (intent) {
            SavedIntent.Load, SavedIntent.Refresh -> load()
            is SavedIntent.RemoveFavourite -> removeFavourite(intent.listingId)
            is SavedIntent.OpenListing -> viewModelScope.launch {
                effectChannel.send(SavedEffect.OpenListing(intent.listingId))
            }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { favouritesRepository.getFavourites() }
                .onSuccess { feed ->
                    _state.update { it.copy(listings = feed.listings, isLoading = false) }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message.orEmpty()) }
                }
        }
    }

    private fun removeFavourite(listingId: String) {
        val previousListings = _state.value.listings
        _state.update {
            it.copy(
                listings = it.listings.filter { l -> l.id != listingId },
                removingIds = it.removingIds + listingId,
            )
        }
        viewModelScope.launch {
            runCatching { favouritesRepository.removeFavourite(listingId) }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(listings = previousListings) }
                }
            _state.update { it.copy(removingIds = it.removingIds - listingId) }
        }
    }
}
