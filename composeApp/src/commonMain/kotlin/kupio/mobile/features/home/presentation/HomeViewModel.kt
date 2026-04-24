package kupio.mobile.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.home.domain.repository.CategoriesRepository
import kupio.mobile.features.home.domain.repository.ListingsRepository

class HomeViewModel(
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

    init {
        loadCategories()
        loadRecommended()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchQueryChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is HomeIntent.SelectCategory -> _state.update { it.copy(selectedCategoryId = intent.id) }
            HomeIntent.RetryLoadListings -> loadRecommended()
            HomeIntent.RetryLoadCategories -> loadCategories()
            else -> Unit
        }
    }

    private fun loadRecommended() {
        _state.update { it.copy(isLoadingListings = true, listingsError = null) }
        viewModelScope.launch {
            runCatching { listingsRepository.getFeed(limit = 10) }
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
            runCatching { categoriesRepository.getRootCategories() }
                .onSuccess { list ->
                    _state.update { it.copy(categories = list, isLoadingCategories = false) }
                }
                .onFailure { t ->
                    _state.update { it.copy(isLoadingCategories = false, categoriesError = t.message.orEmpty()) }
                }
        }
    }
}
