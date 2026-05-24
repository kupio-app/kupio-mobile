package kupio.mobile.features.listings.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.chats.data.ConversationsStore
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.feed.FeedEffect.*
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import kupio.mobile.features.saved.domain.ToggleFavouriteUseCase
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService

class FeedViewModel(
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val favouritesRepository: FavouritesRepository,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val conversationsStore: ConversationsStore,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val effectChannel = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = effectChannel.receiveAsFlow()

    private var loadRecommendedJob: Job? = null

    init {
        loadCategories()
        loadRecommended()
        loadFavouriteIds()
    }

    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is FeedIntent.SelectCategory -> {
                if (intent.id == _state.value.selectedCategoryId) return
                _state.update { it.copy(selectedCategoryId = intent.id) }
                analytics.logEvent("select_content", mapOf("content_type" to "category", "item_id" to intent.id))
                loadRecommended(reset = true)
            }
            is FeedIntent.OpenListing -> viewModelScope.launch {
                analytics.logEvent("select_item", mapOf("item_id" to intent.id))
                effectChannel.send(OpenListing(intent.id))
            }
            FeedIntent.SubmitSearch -> {
                val query = _state.value.searchQuery
                if (query.isBlank()) return
                analytics.logEvent("search", mapOf("search_term" to query))
                viewModelScope.launch { effectChannel.send(OpenSearch(query)) }
            }
            FeedIntent.OpenSearchBar -> viewModelScope.launch { effectChannel.send(OpenSearch("")) }
            FeedIntent.RetryLoadListings -> loadRecommended(reset = true)
            FeedIntent.RetryLoadCategories -> loadCategories()
            FeedIntent.RefreshFeed -> refreshFeed()
            FeedIntent.LoadMore -> {
                if (!_state.value.isLoadingMore && _state.value.hasMore) loadRecommended(reset = false)
            }
            FeedIntent.OpenFilters -> viewModelScope.launch { effectChannel.send(OpenSearchFilters) }
            FeedIntent.OpenNotifications -> {}
            FeedIntent.SelectDelivery -> {}
            is FeedIntent.ToggleFavourite -> toggleFavourite(intent.listingId)
        }
    }

    private fun loadRecommended(reset: Boolean = true) {
        loadRecommendedJob?.cancel()
        val cursor = if (reset) null else _state.value.nextCursor
        if (reset) {
            _state.update { it.copy(isLoadingListings = true, listingsError = null, nextCursor = null, hasMore = false) }
        } else {
            _state.update { it.copy(isLoadingMore = true) }
        }
        loadRecommendedJob = viewModelScope.launch {
            runCatching {
                val categoryId = _state.value.selectedCategoryId
                    .takeUnless { it == FeedCategoryItem.ALL_ID }
                    ?.removePrefix("cat_")
                    ?.toIntOrNull()
                listingsRepository.getFeed(limit = 20, cursor = cursor, categoryId = categoryId)
            }.onSuccess { feed ->
                _state.update { current ->
                    val accumulated = if (reset) feed.listings else current.listings + feed.listings
                    current.copy(
                        listings = accumulated,
                        isLoadingListings = false,
                        isLoadingMore = false,
                        listingsError = null,
                        nextCursor = feed.nextCursor,
                        hasMore = feed.nextCursor != null,
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                if (reset) {
                    _state.update { it.copy(isLoadingListings = false, listingsError = t.message.orEmpty()) }
                } else {
                    _state.update { it.copy(isLoadingMore = false) }
                }
                analytics.recordException(t, mapOf("screen" to "feed"))
            }
        }
    }

    private fun loadCategories() {
        _state.update { it.copy(isLoadingCategories = true, categoriesError = null) }
        viewModelScope.launch {
            fetchCategories()
                .onSuccess { list ->
                    _state.update { it.copy(categories = list, isLoadingCategories = false) }
                }
                .onFailure { t ->
                    _state.update { it.copy(isLoadingCategories = false, categoriesError = t.message.orEmpty()) }
                }
        }
    }

    private fun loadFavouriteIds() {
        viewModelScope.launch {
            runCatching { favouritesRepository.getFavouriteIds() }
                .onSuccess { ids -> _state.update { it.copy(favouritedIds = ids) } }
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
        val event = if (adding) "add_to_favourites" else "remove_from_favourites"
        analytics.logEvent(event, mapOf("item_id" to listingId))
        viewModelScope.launch {
            toggleFavouriteUseCase(listingId, adding).onFailure {
                _state.update {
                    it.copy(favouritedIds = if (adding) it.favouritedIds - listingId else it.favouritedIds + listingId)
                }
            }
            _state.update { it.copy(togglingFavouriteIds = it.togglingFavouriteIds - listingId) }
        }
    }

    private fun refreshFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                coroutineScope {
                    val listingsDeferred = async {
                        runCatching {
                            val categoryId = _state.value.selectedCategoryId
                                .takeUnless { it == FeedCategoryItem.ALL_ID }
                                ?.removePrefix("cat_")
                                ?.toIntOrNull()
                            listingsRepository.getFeed(limit = 20, cursor = null, categoryId = categoryId)
                        }.onFailure { if (it is CancellationException) throw it }
                    }
                    val categoriesDeferred = async { fetchCategories() }
                    val unreadDeferred = async { conversationsStore.refreshUnreadCount() }
                    listingsDeferred.await()
                        .onSuccess { feed ->
                            _state.update { it.copy(
                                listings = feed.listings,
                                listingsError = null,
                                nextCursor = feed.nextCursor,
                                hasMore = feed.nextCursor != null,
                            ) }
                        }
                        .onFailure { t -> _state.update { it.copy(listingsError = t.message.orEmpty()) } }
                    categoriesDeferred.await()
                        .onSuccess { list -> _state.update { it.copy(categories = list, categoriesError = null) } }
                        .onFailure { t -> _state.update { it.copy(categoriesError = t.message.orEmpty()) } }
                    unreadDeferred.await()
                }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchCategories(): Result<List<Category>> = runCatching {
        categoriesRepository.getRootCategories()
    }.onFailure { if (it is CancellationException) throw it }
}
