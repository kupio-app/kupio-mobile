package kupio.mobile.features.listings.presentation.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.ui.graphics.vector.ImageVector
import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.presentation.components.iconForCategorySlug

data class FeedState(
    val deliveryLocation: String = "Bratislava, SK",
    val hasUnreadNotifications: Boolean = true,
    val searchQuery: String = "",
    val categories: List<Category> = emptyList(),
    val isLoadingCategories: Boolean = true,
    val categoriesError: String? = null,
    val selectedCategoryId: String = FeedCategoryItem.ALL_ID,
    val listings: List<Listing> = emptyList(),
    val isLoadingListings: Boolean = true,
    val listingsError: String? = null,
    val isRefreshing: Boolean = false,
    val favouritedIds: Set<String> = emptySet(),
    val togglingFavouriteIds: Set<String> = emptySet(),
) : UiState

data class FeedCategoryItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
) {
    companion object {
        const val ALL_ID = "all"
    }
}

fun buildCategoryItems(categories: List<Category>, allLabel: String): List<FeedCategoryItem> {
    val allItem = FeedCategoryItem(
        id = FeedCategoryItem.ALL_ID,
        label = allLabel,
        icon = Icons.Outlined.GridView,
    )
    val backendItems = categories.map { cat ->
        FeedCategoryItem(
            id = "cat_${cat.id}",
            label = cat.name,
            icon = iconForCategorySlug(cat.iconSlug),
        )
    }
    return listOf(allItem) + backendItems
}

sealed interface FeedIntent : UiAction {
    data object SelectDelivery : FeedIntent
    data object OpenNotifications : FeedIntent
    data class SearchQueryChanged(val query: String) : FeedIntent
    data object SubmitSearch : FeedIntent
    data object OpenSearchBar : FeedIntent
    data object OpenFilters : FeedIntent
    data class SelectCategory(val id: String) : FeedIntent
    data class OpenListing(val id: String) : FeedIntent
    data object RetryLoadListings : FeedIntent
    data object RetryLoadCategories : FeedIntent
    data object RefreshFeed : FeedIntent
    data class ToggleFavourite(val listingId: String) : FeedIntent
}

sealed interface FeedEffect : UiEffect {
    data class OpenListing(val id: String) : FeedEffect
    data class OpenSearch(val query: String) : FeedEffect
}
