package kupio.mobile.features.search.presentation.filters

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.SearchSortBy

data class SearchFiltersState(
    val draft: SearchFilters = SearchFilters(),
    val resultCount: Int? = null,
    val categoryPath: List<Category> = emptyList(),
    val visibleSubcategories: List<Category> = emptyList(),
    val topLevelCategories: List<Category> = emptyList(),
    val isLoadingSubcategories: Boolean = false,
    val subcategoriesError: String? = null,
    val filterDefinitions: List<FilterDefinition> = emptyList(),
    val isLoadingFilters: Boolean = false,
    val filtersError: String? = null,
    val filterValues: Map<String, SearchFilterInput> = emptyMap(),
    val isPriceRangeInvalid: Boolean = false,
) : UiState {
    val selectedCategoryId: Int? get() = draft.categoryId
    val selectedCategoryName: String? get() = draft.categoryName
}

sealed interface SearchFilterInput {
    data class Text(val value: String) : SearchFilterInput
    data class BooleanValue(val value: Boolean?) : SearchFilterInput
}

sealed interface SearchFiltersIntent : UiAction {
    data class QueryChanged(val value: String) : SearchFiltersIntent
    data class CategorySelected(val id: Int) : SearchFiltersIntent
    data object CategoryPickerBack : SearchFiltersIntent
    data object CategoryPickerReset : SearchFiltersIntent
    data class PriceMinChanged(val value: String) : SearchFiltersIntent
    data class PriceMaxChanged(val value: String) : SearchFiltersIntent
    data class PriceRangeChanged(val min: Int, val max: Int) : SearchFiltersIntent
    data class DealTypeSelected(val dealType: DealType) : SearchFiltersIntent
    data class FilterTextChanged(val slug: String, val value: String) : SearchFiltersIntent
    data class FilterBooleanChanged(val slug: String, val value: Boolean?) : SearchFiltersIntent
    data object OnlyWithPhotosToggled : SearchFiltersIntent
    data class SortBySelected(val sortBy: SearchSortBy) : SearchFiltersIntent
    data object Apply : SearchFiltersIntent
    data object ClearAll : SearchFiltersIntent
    data object Back : SearchFiltersIntent
}

sealed interface SearchFiltersEffect : UiEffect {
    data object NavigateBack : SearchFiltersEffect
    data object NavigateToResults : SearchFiltersEffect
}
