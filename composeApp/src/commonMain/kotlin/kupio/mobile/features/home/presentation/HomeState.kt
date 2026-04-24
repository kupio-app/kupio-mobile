package kupio.mobile.features.home.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.ui.graphics.vector.ImageVector
import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.home.domain.model.Category
import kupio.mobile.features.home.domain.model.Listing
import kupio.mobile.features.home.presentation.components.iconForCategorySlug

data class HomeState(
    val deliveryLocation: String = "Bratislava, SK",
    val hasUnreadNotifications: Boolean = true,
    val searchQuery: String = "",
    val categories: List<Category> = emptyList(),
    val isLoadingCategories: Boolean = true,
    val categoriesError: String? = null,
    val selectedCategoryId: String = HomeCategoryItem.ALL_ID,
    val listings: List<Listing> = emptyList(),
    val isLoadingListings: Boolean = true,
    val listingsError: String? = null,
    val isRefreshing: Boolean = false,
) : UiState

data class HomeCategoryItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
) {
    companion object {
        const val ALL_ID = "all"
    }
}

fun buildCategoryItems(categories: List<Category>, allLabel: String): List<HomeCategoryItem> {
    val allItem = HomeCategoryItem(
        id = HomeCategoryItem.ALL_ID,
        label = allLabel,
        icon = Icons.Outlined.GridView,
    )
    val backendItems = categories.map { cat ->
        HomeCategoryItem(
            id = "cat_${cat.id}",
            label = cat.name,
            icon = iconForCategorySlug(cat.iconSlug),
        )
    }
    return listOf(allItem) + backendItems
}

sealed interface HomeIntent : UiAction {
    data object SelectDelivery : HomeIntent
    data object OpenNotifications : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data object SubmitSearch : HomeIntent
    data object OpenFilters : HomeIntent
    data class SelectCategory(val id: String) : HomeIntent
    data class OpenListing(val id: String) : HomeIntent
    data object RetryLoadListings : HomeIntent
    data object RetryLoadCategories : HomeIntent
    data object RefreshFeed : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class OpenListing(val id: String) : HomeEffect
    data class OpenSearch(val query: String) : HomeEffect
}
