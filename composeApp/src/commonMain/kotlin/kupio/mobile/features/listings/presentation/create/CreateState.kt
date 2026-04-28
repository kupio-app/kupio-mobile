package kupio.mobile.features.listings.presentation.create

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.FilterDefinition

data class CreateState(
    val images: List<SelectedListingImage> = emptyList(),
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val currency: Currency = Currency.EUR,
    val isFree: Boolean = false,
    val isTradable: Boolean = false,
    val categories: List<Category> = emptyList(),
    val categoryPath: List<Category> = emptyList(),
    val visibleSubcategories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val selectedCategoryName: String? = null,
    val isLoadingCategories: Boolean = true,
    val categoriesError: CreateText? = null,
    val isLoadingSubcategories: Boolean = false,
    val subcategoriesError: CreateText? = null,
    val filters: List<FilterDefinition> = emptyList(),
    val filterValues: Map<String, CreateFilterInput> = emptyMap(),
    val isLoadingFilters: Boolean = false,
    val filtersError: CreateText? = null,
    val fieldErrors: Map<CreateField, CreateText> = emptyMap(),
    val filterErrors: Map<String, CreateText> = emptyMap(),
    val imageWarning: CreateText? = null,
    val submitError: CreateText? = null,
    val isSubmitting: Boolean = false,
) : UiState {
    val canSubmit: Boolean
        get() = !isSubmitting &&
            !isLoadingCategories &&
            !isLoadingFilters &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            (isFree || price.isNotBlank()) &&
            selectedCategoryId != null
}

sealed interface CreateFilterInput {
    data class Text(val value: String) : CreateFilterInput
    data class BooleanValue(val value: Boolean?) : CreateFilterInput
}

enum class CreateField {
    TITLE,
    DESCRIPTION,
    PRICE,
    CATEGORY,
    CUSTOM_FILTERS,
}

sealed interface CreateIntent : UiAction {
    data object ImageLimitReached : CreateIntent
    data class ImagesSelected(val images: List<SelectedListingImage>) : CreateIntent
    data class RemoveImage(val id: String) : CreateIntent
    data class TitleChanged(val value: String) : CreateIntent
    data class DescriptionChanged(val value: String) : CreateIntent
    data class PriceChanged(val value: String) : CreateIntent
    data class CurrencyChanged(val value: Currency) : CreateIntent
    data class CategorySelected(val id: Int) : CreateIntent
    data object CategoryPickerReset : CreateIntent
    data object CategoryPickerBack : CreateIntent
    data object RetrySubcategories : CreateIntent
    data class FilterTextChanged(val slug: String, val value: String) : CreateIntent
    data class FilterBooleanChanged(val slug: String, val value: Boolean?) : CreateIntent
    data object ToggleFree : CreateIntent
    data object ToggleTradable : CreateIntent
    data object RetryCategories : CreateIntent
    data object RetryFilters : CreateIntent
    data object SaveDraft : CreateIntent
    data object Publish : CreateIntent
    data object Back : CreateIntent
}

sealed interface CreateEffect : UiEffect {
    data object NavigateBack : CreateEffect
}
