package kupio.mobile.features.listings.presentation.create.components

import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioCategoryPickerSheet
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_all_categories
import mobile.composeapp.generated.resources.create_categories
import mobile.composeapp.generated.resources.create_category_choose
import mobile.composeapp.generated.resources.create_category_use
import mobile.composeapp.generated.resources.create_error_load_subcategories
import mobile.composeapp.generated.resources.create_subcategories
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreateCategoryPicker(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val atRoot = state.categoryPath.isEmpty()
    KupioCategoryPickerSheet(
        title = stringResource(Res.string.create_category_choose),
        allCategoriesLabel = stringResource(Res.string.create_all_categories),
        groupLabel = stringResource(if (atRoot) Res.string.create_categories else Res.string.create_subcategories),
        confirmLabel = state.selectedCategoryName?.let { stringResource(Res.string.create_category_use, it) },
        errorLabel = stringResource(Res.string.create_error_load_subcategories),
        path = state.categoryPath,
        displayCategories = if (atRoot) state.categories else state.visibleSubcategories,
        selectedCategoryId = if (atRoot) null else state.selectedCategoryId,
        isLoading = state.isLoadingSubcategories,
        hasError = state.subcategoriesError != null,
        onSelectCategory = { onIntent(CreateIntent.CategorySelected(it)) },
        onConfirm = onDismiss,
        onBack = { onIntent(CreateIntent.CategoryPickerBack) },
        onReset = { onIntent(CreateIntent.CategoryPickerReset) },
        onRetry = { onIntent(CreateIntent.RetrySubcategories) },
        onDismiss = onDismiss,
    )
}
