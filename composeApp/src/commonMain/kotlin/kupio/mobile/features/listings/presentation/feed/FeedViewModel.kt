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
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.feed.FeedEffect.*

class FeedViewModel(
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val effectChannel = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = effectChannel.receiveAsFlow()

    private var loadRecommendedJob: Job? = null

    init {
        loadCategories()
        loadRecommended()
    }

    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is FeedIntent.SelectCategory -> {
                if (intent.id == _state.value.selectedCategoryId) return
                _state.update { it.copy(selectedCategoryId = intent.id) }
                loadRecommended()
            }
            is FeedIntent.OpenListing -> viewModelScope.launch {
                effectChannel.send(OpenListing(intent.id))
            }
            FeedIntent.SubmitSearch -> {
                val query = _state.value.searchQuery
                if (query.isBlank()) return
                viewModelScope.launch { effectChannel.send(OpenSearch(query)) }
            }
            FeedIntent.RetryLoadListings -> loadRecommended()
            FeedIntent.RetryLoadCategories -> loadCategories()
            FeedIntent.RefreshFeed -> refreshFeed()
            FeedIntent.OpenFilters -> {}
            FeedIntent.OpenNotifications -> {}
            FeedIntent.SelectDelivery -> {}
        }
    }

    private fun loadRecommended() {
        loadRecommendedJob?.cancel()
        _state.update { it.copy(isLoadingListings = true, listingsError = null) }
        loadRecommendedJob = viewModelScope.launch {
            fetchRecommended()
                .onSuccess { feed ->
                    _state.update { it.copy(listings = feed.listings, isLoadingListings = false) }
                }
                .onFailure { t ->
                    _state.update { it.copy(isLoadingListings = false, listingsError = t.message.orEmpty()) }
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

    private fun refreshFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                coroutineScope {
                    val listingsDeferred = async { fetchRecommended() }
                    val categoriesDeferred = async { fetchCategories() }
                    listingsDeferred.await()
                        .onSuccess { feed -> _state.update { it.copy(listings = feed.listings, listingsError = null) } }
                        .onFailure { t -> _state.update { it.copy(listingsError = t.message.orEmpty()) } }
                    categoriesDeferred.await()
                        .onSuccess { list -> _state.update { it.copy(categories = list, categoriesError = null) } }
                        .onFailure { t -> _state.update { it.copy(categoriesError = t.message.orEmpty()) } }
                }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchRecommended(): Result<ListingFeed> = runCatching {
        val categoryId = _state.value.selectedCategoryId
            .takeUnless { it == FeedCategoryItem.ALL_ID }
            ?.removePrefix("cat_")
            ?.toIntOrNull()
        listingsRepository.getFeed(limit = 10, categoryId = categoryId)
    }.onFailure { if (it is CancellationException) throw it }

    private suspend fun fetchCategories(): Result<List<Category>> = runCatching {
        categoriesRepository.getRootCategories()
    }.onFailure { if (it is CancellationException) throw it }
}
