package kupio.mobile.features.search.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kupio.mobile.features.search.domain.model.SearchFilters

class SearchSharedState {
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    fun updateFilters(filters: SearchFilters) {
        _filters.value = filters
    }

    fun reset() {
        _filters.value = SearchFilters()
    }
}
