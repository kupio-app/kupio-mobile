package kupio.mobile.features.search.presentation.queries

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.search.domain.model.RecentSearch
import kupio.mobile.features.search.domain.model.SearchFilters

data class SearchQueriesState(
    val query: String = "",
    val recentSearches: List<RecentSearch> = emptyList(),
    val popularNearYou: List<String> = emptyList(),
) : UiState

sealed interface SearchQueriesIntent : UiAction {
    data class QueryChanged(val value: String) : SearchQueriesIntent
    data object Submit : SearchQueriesIntent
    data class TapRecent(val search: RecentSearch) : SearchQueriesIntent
    data class TapRecentWithFilters(val search: RecentSearch) : SearchQueriesIntent
    data class RemoveRecent(val query: String) : SearchQueriesIntent
    data object OpenFilters : SearchQueriesIntent
    data object Back : SearchQueriesIntent
}

sealed interface SearchQueriesEffect : UiEffect {
    data class NavigateToResults(val filters: SearchFilters) : SearchQueriesEffect
    data object NavigateToFilters : SearchQueriesEffect
    data object NavigateBack : SearchQueriesEffect
}
