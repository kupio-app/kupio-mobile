package kupio.mobile.features.saved.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import kupio.mobile.core.presentation.SnackbarEvent
import kupio.mobile.core.presentation.SnackbarManager
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.favourite_removed
import mobile.composeapp.generated.resources.undo
import org.jetbrains.compose.resources.getString

class SavedViewModel(
    private val favouritesRepository: FavouritesRepository,
    private val snackbarManager: SnackbarManager,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
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
            SavedIntent.Load -> load(reset = true)
            SavedIntent.Refresh -> load(reset = true, isRefresh = true)
            SavedIntent.LoadMore -> {
                if (!_state.value.isLoadingMore && _state.value.hasMore) load(reset = false)
            }
            is SavedIntent.RemoveFavourite -> removeFavourite(intent.listingId)
            is SavedIntent.OpenListing -> viewModelScope.launch {
                effectChannel.send(SavedEffect.OpenListing(intent.listingId))
            }
        }
    }

    private fun load(reset: Boolean = true, isRefresh: Boolean = false) {
        val cursor = if (reset) null else _state.value.nextCursor
        when {
            isRefresh -> _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            reset -> _state.update { it.copy(isLoading = true, errorMessage = null, nextCursor = null, hasMore = false) }
            else -> _state.update { it.copy(isLoadingMore = true) }
        }
        viewModelScope.launch {
            runCatching { favouritesRepository.getFavourites(cursor = cursor) }
                .onSuccess { feed ->
                    _state.update { current ->
                        val accumulated = if (reset) feed.listings else current.listings + feed.listings
                        current.copy(
                            listings = accumulated,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            nextCursor = feed.nextCursor,
                            hasMore = feed.nextCursor != null,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    if (reset) {
                        _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = t.message.orEmpty()) }
                    } else {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                    analytics.recordException(t, mapOf("screen" to "saved"))
                }
        }
    }

    private fun removeFavourite(listingId: String) {
        val currentListings = _state.value.listings
        val removedIndex = currentListings.indexOfFirst { it.id == listingId }
        if (removedIndex < 0) return
        val removedListing = currentListings[removedIndex]
        _state.update {
            it.copy(
                listings = it.listings.filter { l -> l.id != listingId },
                removingIds = it.removingIds + listingId,
            )
        }
        analytics.logEvent("remove_from_favourites", mapOf("item_id" to listingId))
        viewModelScope.launch {
            snackbarManager.show(
                SnackbarEvent(
                    icon = Icons.Outlined.FavoriteBorder,
                    message = getString(Res.string.favourite_removed),
                    actionLabel = getString(Res.string.undo),
                    onAction = { undoRemove(listingId, removedListing, removedIndex) },
                )
            )
        }
        viewModelScope.launch {
            runCatching { favouritesRepository.removeFavourite(listingId) }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update {
                        val insertIndex = removedIndex.coerceAtMost(it.listings.size)
                        it.copy(listings = it.listings.toMutableList().also { list -> list.add(insertIndex, removedListing) })
                    }
                }
            _state.update { it.copy(removingIds = it.removingIds - listingId) }
        }
    }

    private fun undoRemove(listingId: String, removedListing: Listing, removedIndex: Int) {
        _state.update {
            val insertIndex = removedIndex.coerceAtMost(it.listings.size)
            it.copy(listings = it.listings.toMutableList().also { list -> list.add(insertIndex, removedListing) })
        }
        viewModelScope.launch {
            runCatching { favouritesRepository.addFavourite(listingId) }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(listings = it.listings.filter { l -> l.id != listingId }) }
                }
        }
    }
}
