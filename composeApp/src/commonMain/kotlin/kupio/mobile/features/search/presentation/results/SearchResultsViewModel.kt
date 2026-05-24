package kupio.mobile.features.search.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.saved.domain.ToggleFavouriteUseCase
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import kupio.mobile.features.search.domain.SearchSharedState
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.SearchSortBy
import kupio.mobile.features.search.domain.repository.SearchHistoryRepository
import kupio.mobile.features.search.presentation.results.SearchResultsEffect.NavigateBack
import kupio.mobile.features.search.presentation.results.SearchResultsEffect.NavigateToFilters
import kupio.mobile.features.search.presentation.results.SearchResultsEffect.NavigateToListing

class SearchResultsViewModel(
    private val listingsRepository: ListingsRepository,
    private val favouritesRepository: FavouritesRepository,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val searchSharedState: SearchSharedState,
    private val searchHistoryRepository: SearchHistoryRepository,
    initialFilters: SearchFilters,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchResultsState(filters = initialFilters))
    val state: StateFlow<SearchResultsState> = _state.asStateFlow()

    private val effectChannel = Channel<SearchResultsEffect>(Channel.BUFFERED)
    val effects: Flow<SearchResultsEffect> = effectChannel.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        searchSharedState.updateFilters(initialFilters)
        observeFavouriteIds()
        observeFilters()
        saveToHistory(initialFilters)
        fetch(reset = true)
    }

    fun onIntent(intent: SearchResultsIntent) {
        when (intent) {
            is SearchResultsIntent.OpenListing -> viewModelScope.launch {
                effectChannel.send(NavigateToListing(intent.id))
            }
            SearchResultsIntent.OpenFilters -> viewModelScope.launch {
                effectChannel.send(NavigateToFilters)
            }
            SearchResultsIntent.LoadMore -> {
                if (!_state.value.isLoadingMore && _state.value.hasMore) fetch(reset = false)
            }
            SearchResultsIntent.Retry -> fetch(reset = true)
            SearchResultsIntent.Back -> viewModelScope.launch { effectChannel.send(NavigateBack) }
            is SearchResultsIntent.QueryChanged -> {
                _state.update { it.copy(filters = it.filters.copy(query = intent.value)) }
            }
            SearchResultsIntent.Submit -> {
                val current = _state.value.filters
                searchSharedState.updateFilters(current)
            }
            is SearchResultsIntent.ToggleFavourite -> toggleFavourite(intent.listingId)
            is SearchResultsIntent.SelectSortBy -> _state.update { current ->
                current.copy(
                    activeSortBy = intent.sortBy,
                    listings = sortListings(current.rawListings, intent.sortBy),
                )
            }
            is SearchResultsIntent.RemoveFilter -> removeFilter(intent.filterKey)
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            searchSharedState.filters
                .drop(1)
                .collect { newFilters ->
                    _state.update { it.copy(filters = newFilters) }
                    saveToHistory(newFilters)
                    fetch(reset = true)
                }
        }
    }

    private fun fetch(reset: Boolean) {
        searchJob?.cancel()
        val cursor = if (reset) null else _state.value.nextCursor
        if (reset) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isLoadingMore = true) }
        }
        searchJob = viewModelScope.launch {
            runCatching {
                listingsRepository.searchListings(
                    filters = _state.value.filters,
                    cursor = cursor,
                )
            }.onSuccess { feed ->
                handleSuccess(feed, reset)
            }.onFailure { t ->
                if (t is CancellationException) throw t
                if (reset) {
                    _state.update { it.copy(isLoading = false, error = t.message.orEmpty()) }
                } else {
                    _state.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    private fun handleSuccess(feed: ListingFeed, reset: Boolean) {
        _state.update { current ->
            val raw = if (reset) feed.listings else current.rawListings + feed.listings
            current.copy(
                rawListings = raw,
                listings = sortListings(raw, current.activeSortBy),
                isLoading = false,
                isLoadingMore = false,
                error = null,
                nextCursor = feed.nextCursor,
                hasMore = feed.nextCursor != null,
            )
        }
    }

    private fun sortListings(listings: List<Listing>, sortBy: SearchSortBy) = when (sortBy) {
        SearchSortBy.RECOMMENDED -> listings
        SearchSortBy.NEWEST_FIRST -> listings.sortedByDescending { it.createdAt }
        SearchSortBy.PRICE_LOW_HIGH -> listings.sortedBy { it.price }
        SearchSortBy.PRICE_HIGH_LOW -> listings.sortedByDescending { it.price }
    }

    private fun saveToHistory(filters: SearchFilters) {
        val query = filters.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            runCatching {
                searchHistoryRepository.addSearch(
                    query = query,
                    categoryId = filters.categoryId,
                    categoryName = filters.categoryName,
                )
            }
        }
    }

    private fun observeFavouriteIds() {
        viewModelScope.launch {
            favouritesRepository.favouriteIds.collect { ids ->
                _state.update { it.copy(favouritedIds = ids) }
            }
        }
    }

    private fun toggleFavourite(listingId: String) {
        val adding = listingId !in _state.value.favouritedIds
        _state.update {
            it.copy(
                favouritedIds = if (adding) it.favouritedIds + listingId else it.favouritedIds - listingId,
                togglingFavouriteIds = it.togglingFavouriteIds + listingId,
            )
        }
        viewModelScope.launch {
            toggleFavouriteUseCase(listingId, adding).onFailure {
                _state.update {
                    it.copy(
                        favouritedIds = if (adding) it.favouritedIds - listingId else it.favouritedIds + listingId,
                    )
                }
            }
            _state.update { it.copy(togglingFavouriteIds = it.togglingFavouriteIds - listingId) }
        }
    }

    private fun removeFilter(key: FilterKey) {
        val current = _state.value.filters
        val updated = when (key) {
            FilterKey.Category -> current.copy(categoryId = null, categoryName = null, categoryPath = null, customFilters = emptyMap())
            FilterKey.Price -> current.copy(minPrice = null, maxPrice = null)
            FilterKey.DealType -> current.copy(dealType = DealType.ANY)
            FilterKey.OnlyWithPhotos -> current.copy(onlyWithPhotos = false)
            is FilterKey.Custom -> current.copy(customFilters = current.customFilters - key.slug)
        }
        searchSharedState.updateFilters(updated)
    }
}
