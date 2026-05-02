package kupio.mobile.features.listings.presentation.form

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateField
import kupio.mobile.features.listings.presentation.create.CreateFilterInput
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState

class ListingFormController(
    private val categoriesRepository: CategoriesRepository,
    private val scope: CoroutineScope,
    private val currentState: () -> CreateState,
    private val updateState: ((CreateState) -> CreateState) -> Unit,
    private val onFormChanged: () -> Unit = {},
) {
    private var filtersJob: Job? = null
    private var subcategoriesJob: Job? = null

    fun handle(intent: CreateIntent): Boolean {
        when (intent) {
            is CreateIntent.TitleChanged -> updateField(CreateField.TITLE) { it.copy(title = intent.value) }
            is CreateIntent.DescriptionChanged -> updateField(CreateField.DESCRIPTION) {
                it.copy(description = intent.value)
            }
            is CreateIntent.PriceChanged -> updateField(CreateField.PRICE) { it.copy(price = intent.value) }
            is CreateIntent.CurrencyChanged -> updateForm { it.copy(currency = intent.value) }
            is CreateIntent.CategorySelected -> selectCategory(intent.id)
            CreateIntent.CategoryPickerReset -> resetCategoryPicker()
            CreateIntent.CategoryPickerBack -> navigateCategoryPickerBack()
            CreateIntent.RetrySubcategories -> currentState().categoryPath.lastOrNull()?.id?.let {
                loadSubcategories(it, forceRefresh = true)
            }
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
                loadFilters(it, forceRefresh = true)
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

    fun cancel() {
        filtersJob?.cancel()
        subcategoriesJob?.cancel()
    }

    fun loadCategories() {
        updateState {
            it.copy(
                isLoadingCategories = true,
                categoriesError = null,
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                subcategoriesError = null,
            )
        }
        scope.launch {
            runCatching { categoriesRepository.getRootCategories(limit = 100) }
                .onSuccess { categories ->
                    updateState {
                        it.copy(
                            categories = categories,
                            isLoadingCategories = false,
                            categoriesError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    updateState {
                        it.copy(
                            isLoadingCategories = false,
                            categoriesError = throwable.message.toCreateError(),
                        )
                    }
                }
        }
    }

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

    private fun selectCategory(categoryId: Int) {
        onFormChanged()
        val state = currentState()
        val category = state.findKnownCategory(categoryId)
        val categoryPath = category?.let { state.pathTo(it) } ?: emptyList()
        val isSameCategory = state.selectedCategoryId == categoryId
        updateState {
            it.copy(
                selectedCategoryId = categoryId,
                selectedCategoryName = category?.name ?: it.selectedCategoryName,
                categoryPath = categoryPath,
                visibleSubcategories = emptyList(),
                isLoadingSubcategories = false,
                subcategoriesError = null,
                filters = if (isSameCategory) it.filters else emptyList(),
                filterValues = if (isSameCategory) it.filterValues else emptyMap(),
                filterErrors = if (isSameCategory) it.filterErrors else emptyMap(),
                fieldErrors = it.fieldErrors - CreateField.CATEGORY - CreateField.CUSTOM_FILTERS,
                filtersError = if (isSameCategory) it.filtersError else null,
                submitError = null,
            )
        }
        if (!isSameCategory) {
            loadFilters(categoryId)
        }
        loadSubcategories(categoryId)
    }

    private fun navigateCategoryPickerBack() {
        val path = currentState().categoryPath
        if (path.size <= 1) {
            resetCategoryPicker()
        } else {
            selectCategory(path[path.lastIndex - 1].id)
        }
    }

    private fun resetCategoryPicker() {
        subcategoriesJob?.cancel()
        updateState {
            it.copy(
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                isLoadingSubcategories = false,
                subcategoriesError = null,
            )
        }
    }

    private fun loadSubcategories(
        categoryId: Int,
        forceRefresh: Boolean = false,
    ) {
        subcategoriesJob?.cancel()
        updateState {
            it.copy(
                isLoadingSubcategories = true,
                subcategoriesError = null,
            )
        }
        subcategoriesJob = scope.launch {
            runCatching {
                categoriesRepository.getSubcategories(
                    categoryId = categoryId,
                    limit = 100,
                    forceRefresh = forceRefresh,
                )
            }.onSuccess { subcategories ->
                updateState { state ->
                    if (state.selectedCategoryId != categoryId) {
                        state
                    } else {
                        state.copy(
                            visibleSubcategories = subcategories,
                            isLoadingSubcategories = false,
                            subcategoriesError = null,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                updateState { state ->
                    if (state.selectedCategoryId != categoryId) {
                        state
                    } else {
                        state.copy(
                            isLoadingSubcategories = false,
                            subcategoriesError = throwable.message.toCreateError(),
                        )
                    }
                }
            }
        }
    }

    private fun loadFilters(
        categoryId: Int,
        forceRefresh: Boolean = false,
    ) {
        filtersJob?.cancel()
        updateState { it.copy(isLoadingFilters = true, filtersError = null) }
        filtersJob = scope.launch {
            runCatching { categoriesRepository.getCategoryFilters(categoryId, forceRefresh = forceRefresh) }
                .onSuccess { filters ->
                    updateState {
                        it.copy(
                            filters = filters,
                            isLoadingFilters = false,
                            filtersError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    updateState {
                        it.copy(
                            isLoadingFilters = false,
                            filtersError = throwable.message.toCreateError(),
                        )
                    }
                }
        }
    }
}

private fun CreateState.findKnownCategory(categoryId: Int): Category? =
    categories.firstOrNull { it.id == categoryId }
        ?: categoryPath.firstOrNull { it.id == categoryId }
        ?: visibleSubcategories.firstOrNull { it.id == categoryId }

private fun CreateState.pathTo(category: Category): List<Category> {
    val existingPathIndex = categoryPath.indexOfFirst { it.id == category.id }
    if (existingPathIndex >= 0) return categoryPath.take(existingPathIndex + 1)

    return if (category.parentId == categoryPath.lastOrNull()?.id) {
        categoryPath + category
    } else {
        listOf(category)
    }
}

fun String?.toCreateError(): CreateError =
    takeUnless { it.isNullOrBlank() }
        ?.let { CreateError.ServerMessage(it) }
        ?: CreateError.Generic
