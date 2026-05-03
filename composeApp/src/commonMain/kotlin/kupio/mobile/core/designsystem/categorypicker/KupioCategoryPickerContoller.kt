package kupio.mobile.core.designsystem.categorypicker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.repository.CategoriesRepository

data class CategoryPickerControllerState(
    val rootCategories: List<Category> = emptyList(),
    val isLoadingRootCategories: Boolean = true,
    val rootCategoriesError: String? = null,
    val categoryPath: List<Category> = emptyList(),
    val visibleSubcategories: List<Category> = emptyList(),
    val isLoadingSubcategories: Boolean = false,
    val subcategoriesError: String? = null,
    val filterDefinitions: List<FilterDefinition> = emptyList(),
    val isLoadingFilters: Boolean = false,
    val filtersError: String? = null,
)

class KupioCategoryPickerContoller(
    private val categoriesRepository: CategoriesRepository,
    private val onCategorySelected: (categoryId: Int, categoryName: String) -> Unit,
    private val onCategoryReset: () -> Unit,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CategoryPickerControllerState())
    val state = _state.asStateFlow()

    private var subcategoriesJob: Job? = null
    private var filtersJob: Job? = null

    fun retrySubcategories() {
        _state.value.categoryPath.lastOrNull()?.id?.let {
            loadSubcategories(it, forceRefresh = true)
        }
    }

    fun retryFilters(categoryId: Int) {
        loadFilters(categoryId, forceRefresh = true)
    }

    fun loadCategories() {
        _state.update {
            it.copy(
                isLoadingRootCategories = true,
                rootCategoriesError = null,
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                subcategoriesError = null,
            )
        }
        scope.launch {
            runCatching { categoriesRepository.getRootCategories(limit = 100) }
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            rootCategories = categories,
                            isLoadingRootCategories = false,
                            rootCategoriesError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _state.update {
                        it.copy(
                            isLoadingRootCategories = false,
                            rootCategoriesError = throwable.message.orEmpty().ifBlank { "Unknown error" }
                        )
                    }
                }
        }
    }

    fun selectCategory(id: Int) {
        val current = _state.value
        val category = current.findKnownCategory(id)
        val newPath = category?.let { current.pathTo(it) } ?: current.categoryPath
        _state.update {
            it.copy(
                categoryPath = newPath,
                visibleSubcategories = emptyList()
            )
        }
        onCategorySelected(id, category?.name.orEmpty())
        loadSubcategories(id)
        loadFilters(id)
    }

    fun navigateBack() {
        val path = state.value.categoryPath
        if (path.size <= 1) {
            reset()
        } else {
            selectCategory(path[path.lastIndex - 1].id)
        }
    }

    fun reset() {
        subcategoriesJob?.cancel()
        _state.update {
            it.copy(
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                isLoadingSubcategories = false,
                subcategoriesError = null,
            )
        }
        onCategoryReset()
    }

    fun loadSubcategories(
        categoryId: Int,
        forceRefresh: Boolean = false,
    ) {
        subcategoriesJob?.cancel()
        _state.update {
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
                _state.update { state ->
                    if (state.categoryPath.lastOrNull()?.id != categoryId) {
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
                _state.update { state ->
                    if (state.categoryPath.lastOrNull()?.id != categoryId) {
                        state
                    } else {
                        state.copy(
                            isLoadingSubcategories = false,
                            subcategoriesError = throwable.message.orEmpty().ifBlank { "Unknown error" }
                        )
                    }
                }
            }
        }
    }

    fun loadFilters(
        categoryId: Int,
        forceRefresh: Boolean = false,
    ) {
        filtersJob?.cancel()
        _state.update { it.copy(isLoadingFilters = true, filtersError = null) }
        filtersJob = scope.launch {
            runCatching { categoriesRepository.getCategoryFilters(categoryId, forceRefresh = forceRefresh) }
                .onSuccess { filters ->
                    _state.update {
                        it.copy(
                            filterDefinitions = filters,
                            isLoadingFilters = false,
                            filtersError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _state.update {
                        it.copy(
                            isLoadingFilters = false,
                            filtersError = throwable.message.orEmpty().ifBlank { "Unknown error" }
                        )
                    }
                }
        }
    }

    fun cancel() {
        filtersJob?.cancel()
        subcategoriesJob?.cancel()
    }
}


private fun CategoryPickerControllerState.findKnownCategory(id: Int): Category? =
    rootCategories.firstOrNull { it.id == id }
        ?: categoryPath.firstOrNull { it.id == id }
        ?: visibleSubcategories.firstOrNull { it.id == id }

private fun CategoryPickerControllerState.pathTo(category: Category): List<Category> {
    val existingIndex = categoryPath.indexOfFirst { it.id == category.id }
    if (existingIndex >= 0) return categoryPath.take(existingIndex + 1)
    return if (category.parentId == categoryPath.lastOrNull()?.id) {
        categoryPath + category
    } else {
        listOf(category)
    }
}