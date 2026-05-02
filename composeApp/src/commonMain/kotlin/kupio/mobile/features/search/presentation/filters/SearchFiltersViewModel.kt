package kupio.mobile.features.search.presentation.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.search.domain.SearchSharedState
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.SearchSortBy
import kupio.mobile.features.search.presentation.filters.SearchFiltersEffect.NavigateBack
import kupio.mobile.features.search.presentation.filters.SearchFiltersEffect.NavigateToResults

class SearchFiltersViewModel(
    private val categoriesRepository: CategoriesRepository,
    private val searchSharedState: SearchSharedState,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchFiltersState())
    val state: StateFlow<SearchFiltersState> = _state.asStateFlow()

    private val effectChannel = Channel<SearchFiltersEffect>(Channel.BUFFERED)
    val effects: Flow<SearchFiltersEffect> = effectChannel.receiveAsFlow()

    init {
        val current = searchSharedState.filters.value
        val filterValues = current.customFilters.mapValues { (_, v) ->
            SearchFilterInput.Text(v) as SearchFilterInput
        }
        _state.update { it.copy(draft = current, filterValues = filterValues) }
        loadTopLevelCategories()
        if (current.categoryId != null) {
            loadCategoryPath(current.categoryId)
        }
    }

    fun onIntent(intent: SearchFiltersIntent) {
        when (intent) {
            is SearchFiltersIntent.QueryChanged ->
                _state.update { it.copy(draft = it.draft.copy(query = intent.value)) }
            SearchFiltersIntent.OpenCategoryPicker ->
                _state.update { it.copy(isCategoryPickerOpen = true) }
            SearchFiltersIntent.CloseCategoryPicker ->
                _state.update { it.copy(isCategoryPickerOpen = false) }
            is SearchFiltersIntent.CategorySelected -> selectCategory(intent.id)
            SearchFiltersIntent.CategoryPickerBack -> navigateCategoryBack()
            SearchFiltersIntent.CategoryPickerReset -> resetCategoryPicker()
            is SearchFiltersIntent.PriceMinChanged -> {
                val v = intent.value.toIntOrNull()
                _state.update { it.copy(draft = it.draft.copy(minPrice = v)) }
            }
            is SearchFiltersIntent.PriceMaxChanged -> {
                val v = intent.value.toIntOrNull()
                _state.update { it.copy(draft = it.draft.copy(maxPrice = v)) }
            }
            is SearchFiltersIntent.PriceRangeChanged ->
                _state.update { it.copy(draft = it.draft.copy(minPrice = intent.min, maxPrice = intent.max)) }
            is SearchFiltersIntent.DealTypeSelected -> {
                val current = _state.value.draft.dealType
                val newType = if (current == intent.dealType) DealType.ANY else intent.dealType
                _state.update { it.copy(draft = it.draft.copy(dealType = newType)) }
            }
            is SearchFiltersIntent.FilterTextChanged ->
                _state.update { it.copy(filterValues = it.filterValues + (intent.slug to SearchFilterInput.Text(intent.value))) }
            is SearchFiltersIntent.FilterBooleanChanged ->
                _state.update { it.copy(filterValues = it.filterValues + (intent.slug to SearchFilterInput.BooleanValue(intent.value))) }
            SearchFiltersIntent.OnlyWithPhotosToggled ->
                _state.update { it.copy(draft = it.draft.copy(onlyWithPhotos = !it.draft.onlyWithPhotos)) }
            SearchFiltersIntent.DeliveryAvailableToggled ->
                _state.update { it.copy(draft = it.draft.copy(deliveryAvailable = !it.draft.deliveryAvailable)) }
            is SearchFiltersIntent.SortBySelected ->
                _state.update { it.copy(draft = it.draft.copy(sortBy = intent.sortBy)) }
            SearchFiltersIntent.Apply -> applyFilters()
            SearchFiltersIntent.ClearAll -> resetAll()
            SearchFiltersIntent.Back -> viewModelScope.launch { effectChannel.send(NavigateBack) }
        }
    }

    private fun applyFilters() {
        val current = _state.value
        val customFilters = current.filterValues.mapNotNull { (slug, input) ->
            when (input) {
                is SearchFilterInput.Text -> if (input.value.isNotBlank()) slug to input.value else null
                is SearchFilterInput.BooleanValue -> input.value?.let { slug to it.toString() }
            }
        }.toMap()
        val finalDraft = current.draft.copy(customFilters = customFilters)
        searchSharedState.updateFilters(finalDraft)
        viewModelScope.launch { effectChannel.send(NavigateToResults) }
    }

    private fun resetAll() {
        _state.update {
            it.copy(
                draft = SearchFilters(query = it.draft.query),
                filterValues = emptyMap(),
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                filterDefinitions = emptyList(),
            )
        }
    }

    private fun loadTopLevelCategories() {
        viewModelScope.launch {
            runCatching { categoriesRepository.getRootCategories() }
                .onSuccess { cats -> _state.update { it.copy(topLevelCategories = cats) } }
                .onFailure { if (it is CancellationException) throw it }
        }
    }

    private fun loadCategoryPath(categoryId: Int) {
        viewModelScope.launch {
            runCatching { buildCategoryPath(categoryId) }
                .onSuccess { path ->
                    _state.update { it.copy(categoryPath = path) }
                    if (path.isNotEmpty()) loadSubcategories(path.last().id)
                }
                .onFailure { if (it is CancellationException) throw it }
        }
    }

    private suspend fun buildCategoryPath(leafId: Int): List<kupio.mobile.features.listings.domain.model.Category> {
        return emptyList()
    }

    private fun selectCategory(id: Int) {
        _state.update { it.copy(isLoadingSubcategories = true, subcategoriesError = null) }
        viewModelScope.launch {
            runCatching { categoriesRepository.getSubcategories(id) }
                .onSuccess { subs ->
                    val currentPath = _state.value.categoryPath
                    val allCategories = if (currentPath.isEmpty()) _state.value.topLevelCategories else _state.value.visibleSubcategories
                    val selected = allCategories.find { it.id == id }
                    val newPath = currentPath + listOfNotNull(selected)
                    if (subs.isEmpty()) {
                        val cat = selected
                        _state.update { s ->
                            s.copy(
                                draft = s.draft.copy(
                                    categoryId = cat?.id,
                                    categoryName = cat?.name,
                                    customFilters = emptyMap(),
                                ),
                                categoryPath = newPath,
                                visibleSubcategories = emptyList(),
                                isLoadingSubcategories = false,
                                filterValues = emptyMap(),
                                isCategoryPickerOpen = false,
                            )
                        }
                        loadFilters(id)
                    } else {
                        _state.update { s ->
                            s.copy(
                                categoryPath = newPath,
                                visibleSubcategories = subs,
                                isLoadingSubcategories = false,
                            )
                        }
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoadingSubcategories = false, subcategoriesError = t.message.orEmpty()) }
                }
        }
    }

    private fun navigateCategoryBack() {
        val path = _state.value.categoryPath
        if (path.isEmpty()) return
        val newPath = path.dropLast(1)
        _state.update { it.copy(categoryPath = newPath) }
        if (newPath.isNotEmpty()) {
            loadSubcategories(newPath.last().id)
        } else {
            _state.update { it.copy(visibleSubcategories = emptyList()) }
        }
    }

    private fun resetCategoryPicker() {
        _state.update { it.copy(categoryPath = emptyList(), visibleSubcategories = emptyList()) }
    }

    private fun loadSubcategories(categoryId: Int) {
        _state.update { it.copy(isLoadingSubcategories = true, subcategoriesError = null) }
        viewModelScope.launch {
            runCatching { categoriesRepository.getSubcategories(categoryId) }
                .onSuccess { subs -> _state.update { it.copy(visibleSubcategories = subs, isLoadingSubcategories = false) } }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoadingSubcategories = false, subcategoriesError = t.message.orEmpty()) }
                }
        }
    }

    private fun loadFilters(categoryId: Int) {
        _state.update { it.copy(isLoadingFilters = true, filtersError = null) }
        viewModelScope.launch {
            runCatching { categoriesRepository.getCategoryFilters(categoryId) }
                .onSuccess { defs -> _state.update { it.copy(filterDefinitions = defs, isLoadingFilters = false) } }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoadingFilters = false, filtersError = t.message.orEmpty()) }
                }
        }
    }
}
