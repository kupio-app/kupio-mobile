package kupio.mobile.features.me.presentation.mylistings

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus

data class MyListingsState(
    val listings: List<OwnedListing> = emptyList(),
    val filter: MyListingsFilter = MyListingsFilter.ACTIVE,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
) : UiState {
    val visibleListings: List<OwnedListing>
        get() = when (filter) {
            MyListingsFilter.ACTIVE -> listings.filter { it.status == OwnedListingStatus.ACTIVE }
            MyListingsFilter.INACTIVE -> listings.filter { it.status == OwnedListingStatus.INACTIVE }
            MyListingsFilter.ALL -> listings
        }
}

enum class MyListingsFilter { ACTIVE, INACTIVE, ALL }

sealed interface MyListingsIntent : UiAction {
    data class FilterSelected(val filter: MyListingsFilter) : MyListingsIntent
    data class EditListing(val id: String) : MyListingsIntent
    data class BumpUp(val id: String) : MyListingsIntent
    data class Promote(val id: String) : MyListingsIntent
    data class ToggleActive(val id: String, val deactivate: Boolean) : MyListingsIntent
    data object BackClicked : MyListingsIntent
}

sealed interface MyListingsEffect : UiEffect {
    data object NavigateBack : MyListingsEffect
}
