package kupio.mobile.features.search.presentation.filters.components

import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioCategoryPickerSheet
import kupio.mobile.features.search.presentation.filters.SearchFiltersIntent
import kupio.mobile.features.search.presentation.filters.SearchFiltersState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_filters_all_categories
import mobile.composeapp.generated.resources.search_filters_categories
import mobile.composeapp.generated.resources.search_filters_category_placeholder
import mobile.composeapp.generated.resources.search_filters_error_load_subcategories
import mobile.composeapp.generated.resources.search_filters_subcategories
import mobile.composeapp.generated.resources.search_filters_use_category
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchCategoryPicker(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val atRoot = state.categoryPath.isEmpty()
    KupioCategoryPickerSheet(
        title = stringResource(Res.string.search_filters_category_placeholder),
        allCategoriesLabel = stringResource(Res.string.search_filters_all_categories),
        groupLabel = stringResource(if (atRoot) Res.string.search_filters_categories else Res.string.search_filters_subcategories),
        confirmLabel = state.selectedCategoryName?.let { stringResource(Res.string.search_filters_use_category, it) },
        errorLabel = stringResource(Res.string.search_filters_error_load_subcategories),
        path = state.categoryPath,
        displayCategories = if (atRoot) state.topLevelCategories else state.visibleSubcategories,
        selectedCategoryId = state.selectedCategoryId,
        isLoading = state.isLoadingSubcategories,
        hasError = state.subcategoriesError != null,
        onSelectCategory = { onIntent(SearchFiltersIntent.CategorySelected(it)) },
        onConfirm = { onIntent(SearchFiltersIntent.CloseCategoryPicker) },
        onBack = { onIntent(SearchFiltersIntent.CategoryPickerBack) },
        onReset = { onIntent(SearchFiltersIntent.CategoryPickerReset) },
        onRetry = {
            val parent = state.categoryPath.lastOrNull()
            if (parent != null) onIntent(SearchFiltersIntent.CategorySelected(parent.id))
            else onIntent(SearchFiltersIntent.CategoryPickerReset)
        },
        onDismiss = { onIntent(SearchFiltersIntent.CloseCategoryPicker) },
    )
}
