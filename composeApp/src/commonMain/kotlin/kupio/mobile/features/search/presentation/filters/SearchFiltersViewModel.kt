package kupio.mobile.features.search.presentation.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.designsystem.categorypicker.CategoryPickerControllerState
import kupio.mobile.core.designsystem.categorypicker.KupioCategoryPickerContoller
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.search.domain.SearchSharedState
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.presentation.filters.SearchFiltersEffect.NavigateBack
import kupio.mobile.features.search.presentation.filters.SearchFiltersEffect.NavigateToResults

class SearchFiltersViewModel(
    categoriesRepository: CategoriesRepository,
    private val searchSharedState: SearchSharedState,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchFiltersState())
    val state: StateFlow<SearchFiltersState> = _state.asStateFlow()

    private val effectChannel = Channel<SearchFiltersEffect>(Channel.BUFFERED)
    val effects: Flow<SearchFiltersEffect> = effectChannel.receiveAsFlow()

    private val categoryPicker = KupioCategoryPickerContoller(
        categoriesRepository,
        onCategorySelected = { id, name ->
            _state.update {
                it.copy(
                    draft = it.draft.copy(categoryId = id, categoryName = name, customFilters = emptyMap()),
                    filterValues = emptyMap(),
                )
            }
        },
        onCategoryReset = {
            _state.update {
                it.copy(
                    draft = it.draft.copy(categoryId = null, categoryName = null, customFilters = emptyMap()),
                    filterValues = emptyMap(),
                )
            }
        },
        scope = viewModelScope,
    )

    init {
        viewModelScope.launch {
            categoryPicker.state.collect { mergePickerState(it) }
        }

        val current = searchSharedState.filters.value
        val filterValues = current.customFilters.mapValues { (_, v) ->
            SearchFilterInput.Text(v) as SearchFilterInput
        }
        _state.update { it.copy(draft = current, filterValues = filterValues ) }
        categoryPicker.loadCategories()
    }

    fun onIntent(intent: SearchFiltersIntent) {
        when (intent) {
            is SearchFiltersIntent.QueryChanged ->
                _state.update { it.copy(draft = it.draft.copy(query = intent.value)) }
            is SearchFiltersIntent.CategorySelected -> categoryPicker.selectCategory(intent.id)
            SearchFiltersIntent.CategoryPickerBack -> categoryPicker.navigateBack()
            SearchFiltersIntent.CategoryPickerReset -> categoryPicker.reset()
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

    private fun mergePickerState(picker: CategoryPickerControllerState) {
        _state.update {
            it.copy(
                topLevelCategories = picker.rootCategories,
                categoryPath = picker.categoryPath,
                visibleSubcategories = picker.visibleSubcategories,
                isLoadingSubcategories = picker.isLoadingSubcategories,
                subcategoriesError = picker.subcategoriesError,
                filterDefinitions = picker.filterDefinitions,
                isLoadingFilters = picker.isLoadingFilters,
                filtersError = picker.filtersError,
            )
        }
    }
}
