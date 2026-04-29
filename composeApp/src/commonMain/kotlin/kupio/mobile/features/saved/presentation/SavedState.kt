package kupio.mobile.features.saved.presentation

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing

data class SavedState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val removingIds: Set<String> = emptySet(),
) : UiState

sealed interface SavedIntent : UiAction {
    data object Load : SavedIntent
    data object Refresh : SavedIntent
    data class RemoveFavourite(val listingId: String) : SavedIntent
    data class OpenListing(val listingId: String) : SavedIntent
}

sealed interface SavedEffect : UiEffect {
    data class OpenListing(val listingId: String) : SavedEffect
}
