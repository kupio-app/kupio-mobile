package kupio.mobile.features.listings.presentation.detail

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing

data class ListingDetailState(
    val listing: Listing? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) : UiState

sealed interface ListingDetailIntent : UiAction {
    data object Retry : ListingDetailIntent
    data object Back : ListingDetailIntent
}

sealed interface ListingDetailEffect : UiEffect {
    data object NavigateBack : ListingDetailEffect
}
