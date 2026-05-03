package kupio.mobile.features.listings.presentation.form

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kupio.mobile.core.designsystem.categorypicker.CategoryPickerControllerState
import kupio.mobile.core.designsystem.categorypicker.KupioCategoryPickerContoller
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateField
import kupio.mobile.features.listings.presentation.create.CreateFilterInput
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState

class ListingFormController(
    categoriesRepository: CategoriesRepository,
    private val scope: CoroutineScope,
    private val currentState: () -> CreateState,
    private val updateState: ((CreateState) -> CreateState) -> Unit,
    private val onFormChanged: () -> Unit = {},
) {
    private val categoryPicker = KupioCategoryPickerContoller(
        categoriesRepository,
        onCategorySelected = { id, name ->
            onFormChanged()
            val isSameCategory = currentState().selectedCategoryId == id
            updateState {
                it.copy(
                    selectedCategoryId = id,
                    selectedCategoryName = name,
                    filters = if (isSameCategory) it.filters else emptyList(),
                    filterValues = if (isSameCategory) it.filterValues else emptyMap(),
                    filterErrors = if (isSameCategory) it.filterErrors else emptyMap(),
                    filtersError = if (isSameCategory) it.filtersError else null,
                    fieldErrors = it.fieldErrors - CreateField.CATEGORY - CreateField.CUSTOM_FILTERS,
                    submitError = null,
                )
            }
        },
        onCategoryReset = {
            onFormChanged()
            updateState {
                it.copy(
                    selectedCategoryId = null,
                    selectedCategoryName = null,
                    filters = emptyList(),
                    filterValues = emptyMap(),
                    filterErrors = emptyMap(),
                    filtersError = null,
                    submitError = null,
                )
            }
        },
        scope = scope,
    )

    init {
        scope.launch {
            categoryPicker.state.collect { pickerState ->
                updateState { it.mergePickerState(pickerState) }
            }
        }
    }

    fun handle(intent: CreateIntent): Boolean {
        when (intent) {
            is CreateIntent.TitleChanged -> updateField(CreateField.TITLE) { it.copy(title = intent.value) }
            is CreateIntent.DescriptionChanged -> updateField(CreateField.DESCRIPTION) {
                it.copy(description = intent.value)
            }
            is CreateIntent.PriceChanged -> updateField(CreateField.PRICE) { it.copy(price = intent.value) }
            is CreateIntent.CurrencyChanged -> updateForm { it.copy(currency = intent.value) }
            is CreateIntent.CategorySelected -> categoryPicker.selectCategory(intent.id)
            CreateIntent.CategoryPickerReset -> categoryPicker.reset()
            CreateIntent.CategoryPickerBack -> categoryPicker.navigateBack()
            CreateIntent.RetrySubcategories -> categoryPicker.retrySubcategories()
            is CreateIntent.FilterTextChanged -> updateFilterText(intent.slug, intent.value)
            is CreateIntent.FilterBooleanChanged -> updateFilterBoolean(intent.slug, intent.value)
            CreateIntent.ToggleFree -> updateForm {
                it.copy(
                    isFree = !it.isFree,
                    price = if (!it.isFree) "0" else it.price,
                    fieldErrors = it.fieldErrors - CreateField.PRICE,
                    submitError = null,
                )
            }
            CreateIntent.ToggleTradable -> updateForm { it.copy(isTradable = !it.isTradable) }
            CreateIntent.RetryCategories -> loadCategories()
            CreateIntent.RetryFilters -> currentState().selectedCategoryId?.let {
                categoryPicker.retryFilters(it)
            }
            CreateIntent.ImageLimitReached,
            is CreateIntent.ImagesSelected,
            is CreateIntent.RemoveImage,
            is CreateIntent.MoveImage,
            CreateIntent.SaveDraft,
            CreateIntent.Publish,
            CreateIntent.Back,
            -> return false
        }
        return true
    }

    fun cancel() = categoryPicker.cancel()
    fun loadCategories() = categoryPicker.loadCategories()

    private fun updateField(
        field: CreateField,
        transform: (CreateState) -> CreateState,
    ) {
        updateForm { state ->
            transform(state).copy(
                fieldErrors = state.fieldErrors - field,
                submitError = null,
            )
        }
    }

    private fun updateFilterText(slug: String, value: String) {
        updateForm {
            it.copy(
                filterValues = it.filterValues + (slug to CreateFilterInput.Text(value)),
                filterErrors = it.filterErrors - slug,
                submitError = null,
            )
        }
    }

    private fun updateFilterBoolean(slug: String, value: Boolean?) {
        updateForm {
            it.copy(
                filterValues = it.filterValues + (slug to CreateFilterInput.BooleanValue(value)),
                filterErrors = it.filterErrors - slug,
                submitError = null,
            )
        }
    }

    private fun updateForm(transform: (CreateState) -> CreateState) {
        onFormChanged()
        updateState { transform(it).copy(submitError = null) }
    }
}

private fun CreateState.mergePickerState(picker: CategoryPickerControllerState): CreateState = copy(
    categories = picker.rootCategories,
    isLoadingCategories = picker.isLoadingRootCategories,
    categoriesError = picker.rootCategoriesError?.toCreateError(),
    categoryPath = picker.categoryPath,
    visibleSubcategories = picker.visibleSubcategories,
    isLoadingSubcategories = picker.isLoadingSubcategories,
    subcategoriesError = picker.subcategoriesError?.toCreateError(),
    filters = picker.filterDefinitions,
    isLoadingFilters = picker.isLoadingFilters,
    filtersError = picker.filtersError?.toCreateError(),
)

fun String?.toCreateError(): CreateError =
    takeUnless { it.isNullOrBlank() }
        ?.let { CreateError.ServerMessage(it) }
        ?: CreateError.Generic
