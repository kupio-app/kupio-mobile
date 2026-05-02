package kupio.mobile.features.search.presentation.queries

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
import kupio.mobile.features.search.domain.SearchSharedState
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.repository.SearchHistoryRepository
import kupio.mobile.features.search.presentation.queries.SearchQueriesEffect.NavigateBack
import kupio.mobile.features.search.presentation.queries.SearchQueriesEffect.NavigateToFilters
import kupio.mobile.features.search.presentation.queries.SearchQueriesEffect.NavigateToResults

private val POPULAR_NEAR_YOU = listOf(
    "iphone", "road bike", "bookshelf", "kids stroller",
    "kitchen table", "wool rug", "sofa", "laptop", "camera",
)

class SearchQueriesViewModel(
    private val searchHistoryRepository: SearchHistoryRepository,
    private val searchSharedState: SearchSharedState,
    initialQuery: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SearchQueriesState(
            query = initialQuery,
            popularNearYou = POPULAR_NEAR_YOU,
        ),
    )
    val state: StateFlow<SearchQueriesState> = _state.asStateFlow()

    private val effectChannel = Channel<SearchQueriesEffect>(Channel.BUFFERED)
    val effects: Flow<SearchQueriesEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            searchHistoryRepository.getRecentSearches().collect { searches ->
                _state.update { it.copy(recentSearches = searches) }
            }
        }
    }

    fun onIntent(intent: SearchQueriesIntent) {
        when (intent) {
            is SearchQueriesIntent.QueryChanged -> _state.update { it.copy(query = intent.value) }
            is SearchQueriesIntent.Submit -> submit()
            is SearchQueriesIntent.TapRecent -> openResults(SearchFilters(
                query = intent.search.query,
                categoryId = intent.search.categoryId,
                categoryName = intent.search.categoryName,
            ))
            is SearchQueriesIntent.TapRecentWithFilters -> {
                searchSharedState.updateFilters(
                    SearchFilters(
                        query = intent.search.query,
                        categoryId = intent.search.categoryId,
                        categoryName = intent.search.categoryName,
                    ),
                )
                viewModelScope.launch { effectChannel.send(NavigateToFilters) }
            }
            is SearchQueriesIntent.RemoveRecent -> viewModelScope.launch {
                searchHistoryRepository.removeSearch(intent.query)
            }
            SearchQueriesIntent.OpenFilters -> viewModelScope.launch { effectChannel.send(NavigateToFilters) }
            SearchQueriesIntent.Back -> viewModelScope.launch { effectChannel.send(NavigateBack) }
        }
    }

    private fun submit() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return
        openResults(SearchFilters(query = query))
    }

    private fun openResults(filters: SearchFilters) {
        viewModelScope.launch { effectChannel.send(NavigateToResults(filters)) }
    }
}
