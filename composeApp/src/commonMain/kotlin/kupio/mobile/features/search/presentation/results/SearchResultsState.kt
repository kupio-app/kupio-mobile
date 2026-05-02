package kupio.mobile.features.search.presentation.results

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.SearchSortBy

data class SearchResultsState(
    val filters: SearchFilters = SearchFilters(),
    val rawListings: List<Listing> = emptyList(),
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val favouritedIds: Set<String> = emptySet(),
    val togglingFavouriteIds: Set<String> = emptySet(),
    val activeSortBy: SearchSortBy = SearchSortBy.RECOMMENDED,
) : UiState

sealed interface SearchResultsIntent : UiAction {
    data class OpenListing(val id: String) : SearchResultsIntent
    data object OpenFilters : SearchResultsIntent
    data object LoadMore : SearchResultsIntent
    data object Retry : SearchResultsIntent
    data object Back : SearchResultsIntent
    data class QueryChanged(val value: String) : SearchResultsIntent
    data object Submit : SearchResultsIntent
    data class ToggleFavourite(val listingId: String) : SearchResultsIntent
    data class SelectSortBy(val sortBy: SearchSortBy) : SearchResultsIntent
    data class RemoveFilter(val filterKey: FilterKey) : SearchResultsIntent
}

sealed interface FilterKey {
    data object Category : FilterKey
    data object Price : FilterKey
    data object DealType : FilterKey
    data object OnlyWithPhotos : FilterKey
    data object DeliveryAvailable : FilterKey
    data class Custom(val slug: String) : FilterKey
}

sealed interface SearchResultsEffect : UiEffect {
    data class NavigateToListing(val id: String) : SearchResultsEffect
    data object NavigateToFilters : SearchResultsEffect
    data object NavigateBack : SearchResultsEffect
}
