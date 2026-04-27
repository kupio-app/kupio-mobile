package kupio.mobile.features.createlisting.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.createlisting.MaxListingImages
import kupio.mobile.features.createlisting.UnsupportedListingImageMessage
import kupio.mobile.features.createlisting.domain.model.SelectedListingImage
import kupio.mobile.features.createlisting.isSupportedListingImageMimeType
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class CreateViewModel(
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateState())
    val state: StateFlow<CreateState> = _state.asStateFlow()

    private val effectChannel = Channel<CreateEffect>(Channel.BUFFERED)
    val effects: Flow<CreateEffect> = effectChannel.receiveAsFlow()

    private var filtersJob: Job? = null
    private var subcategoriesJob: Job? = null

    init {
        loadCategories()
    }

    fun onIntent(intent: CreateIntent) {
        when (intent) {
            CreateIntent.ImageLimitReached -> _state.update {
                it.copy(imageWarning = "You can add up to $MaxListingImages photos.")
            }
            is CreateIntent.ImagesSelected -> addImages(intent.images)
            is CreateIntent.RemoveImage -> _state.update {
                it.copy(
                    images = it.images.filterNot { image -> image.id == intent.id },
                    imageWarning = null,
                )
            }
            is CreateIntent.TitleChanged -> updateField(CreateField.TITLE) { it.copy(title = intent.value) }
            is CreateIntent.DescriptionChanged -> updateField(CreateField.DESCRIPTION) {
                it.copy(description = intent.value)
            }
            is CreateIntent.PriceChanged -> updateField(CreateField.PRICE) { it.copy(price = intent.value) }
            is CreateIntent.CurrencyChanged -> _state.update { it.copy(currency = intent.value) }
            is CreateIntent.CategorySelected -> selectCategory(intent.id)
            CreateIntent.CategoryPickerReset -> resetCategoryPicker()
            CreateIntent.CategoryPickerBack -> navigateCategoryPickerBack()
            CreateIntent.RetrySubcategories -> _state.value.categoryPath.lastOrNull()?.id?.let {
                loadSubcategories(it, forceRefresh = true)
            }
            is CreateIntent.FilterTextChanged -> updateFilterText(intent.slug, intent.value)
            is CreateIntent.FilterBooleanChanged -> updateFilterBoolean(intent.slug, intent.value)
            CreateIntent.ToggleFree -> _state.update {
                it.copy(
                    isFree = !it.isFree,
                    price = if (!it.isFree) "0" else it.price,
                    fieldErrors = it.fieldErrors - CreateField.PRICE,
                )
            }
            CreateIntent.ToggleTradable -> _state.update { it.copy(isTradable = !it.isTradable) }
            CreateIntent.RetryCategories -> loadCategories()
            CreateIntent.RetryFilters -> _state.value.selectedCategoryId?.let { loadFilters(it, forceRefresh = true) }
            CreateIntent.SaveDraft -> submit(activate = false)
            CreateIntent.Publish -> submit(activate = true)
            CreateIntent.Back -> viewModelScope.launch { effectChannel.send(CreateEffect.NavigateBack) }
        }
    }

    private fun addImages(images: List<SelectedListingImage>) {
        if (images.isEmpty()) return
        _state.update { state ->
            val existingIds = state.images.map { it.id }.toSet()
            val supportedImages = images.filter { isSupportedListingImageMimeType(it.mimeType) }
            val newImages = supportedImages.filterNot { it.id in existingIds }
            val remainingSlots = MaxListingImages - state.images.size
            val merged = state.images + newImages.take(remainingSlots.coerceAtLeast(0))
            val hitLimit = remainingSlots <= 0 || newImages.size > remainingSlots
            val hasUnsupportedImages = supportedImages.size != images.size
            state.copy(
                images = merged,
                imageWarning = when {
                    hasUnsupportedImages -> UnsupportedListingImageMessage
                    hitLimit -> "You can add up to $MaxListingImages photos."
                    else -> null
                },
            )
        }
    }

    private fun updateField(
        field: CreateField,
        transform: (CreateState) -> CreateState,
    ) {
        _state.update { state ->
            transform(state).copy(
                fieldErrors = state.fieldErrors - field,
                submitError = null,
            )
        }
    }

    private fun updateFilterText(slug: String, value: String) {
        _state.update { state ->
            state.copy(
                filterValues = state.filterValues + (slug to CreateFilterInput.Text(value)),
                filterErrors = state.filterErrors - slug,
                submitError = null,
            )
        }
    }

    private fun updateFilterBoolean(slug: String, value: Boolean?) {
        _state.update { state ->
            state.copy(
                filterValues = state.filterValues + (slug to CreateFilterInput.BooleanValue(value)),
                filterErrors = state.filterErrors - slug,
                submitError = null,
            )
        }
    }

    private fun selectCategory(categoryId: Int) {
        val state = _state.value
        val category = state.findKnownCategory(categoryId)
        val categoryPath = category?.let { state.pathTo(it) } ?: emptyList()
        val isSameCategory = state.selectedCategoryId == categoryId
        _state.update {
            it.copy(
                selectedCategoryId = categoryId,
                selectedCategoryName = category?.name,
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
        val path = _state.value.categoryPath
        if (path.size <= 1) {
            resetCategoryPicker()
        } else {
            selectCategory(path[path.lastIndex - 1].id)
        }
    }

    private fun resetCategoryPicker() {
        subcategoriesJob?.cancel()
        _state.update {
            it.copy(
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                isLoadingSubcategories = false,
                subcategoriesError = null,
            )
        }
    }

    private fun loadCategories() {
        _state.update {
            it.copy(
                isLoadingCategories = true,
                categoriesError = null,
                categoryPath = emptyList(),
                visibleSubcategories = emptyList(),
                subcategoriesError = null,
            )
        }
        viewModelScope.launch {
            runCatching { categoriesRepository.getRootCategories(limit = 100) }
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            isLoadingCategories = false,
                            categoriesError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _state.update {
                        it.copy(
                            isLoadingCategories = false,
                            categoriesError = throwable.message.orGenericError(),
                        )
                    }
                }
        }
    }

    private fun loadSubcategories(
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
        subcategoriesJob = viewModelScope.launch {
            runCatching {
                categoriesRepository.getSubcategories(
                    categoryId = categoryId,
                    limit = 100,
                    forceRefresh = forceRefresh,
                )
            }.onSuccess { subcategories ->
                _state.update { state ->
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
                _state.update { state ->
                    if (state.selectedCategoryId != categoryId) {
                        state
                    } else {
                        state.copy(
                            isLoadingSubcategories = false,
                            subcategoriesError = throwable.message.orGenericError(),
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
        _state.update { it.copy(isLoadingFilters = true, filtersError = null) }
        filtersJob = viewModelScope.launch {
            runCatching { categoriesRepository.getCategoryFilters(categoryId, forceRefresh = forceRefresh) }
                .onSuccess { filters ->
                    _state.update {
                        it.copy(
                            filters = filters,
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
                            filtersError = throwable.message.orGenericError(),
                        )
                    }
                }
        }
    }

    private fun submit(activate: Boolean) {
        if (_state.value.isSubmitting) return

        if (_state.value.images.any { !isSupportedListingImageMimeType(it.mimeType) }) {
            _state.update {
                it.copy(
                    imageWarning = UnsupportedListingImageMessage,
                    submitError = UnsupportedListingImageMessage,
                )
            }
            return
        }

        val validation = validateCreateListing(_state.value)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    fieldErrors = validation.fieldErrors,
                    filterErrors = validation.filterErrors,
                    submitError = null,
                )
            }
            return
        }

        val listing = validation.listing ?: return
        val images = _state.value.images
        _state.update {
            it.copy(
                isSubmitting = true,
                fieldErrors = emptyMap(),
                filterErrors = emptyMap(),
                submitError = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val created = listingsRepository.createListing(listing)
                listingsRepository.uploadListingImages(
                    listingId = created.id,
                    images = images.map { it.toUpload() },
                )
                if (activate) {
                    listingsRepository.updateListingStatus(
                        listingId = created.id,
                        status = ListingStatus.ACTIVE,
                    )
                }
            }.onSuccess {
                _state.update {
                    CreateState(
                        categories = it.categories,
                        isLoadingCategories = it.isLoadingCategories,
                        categoriesError = it.categoriesError,
                    )
                }
                effectChannel.send(CreateEffect.NavigateBack)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        fieldErrors = throwable.fieldErrors(),
                        submitError = throwable.message.orGenericError(),
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

private fun SelectedListingImage.toUpload(): ListingImageUpload = ListingImageUpload(
    fileName = fileName,
    mimeType = mimeType,
    bytes = bytes,
)

private fun Throwable.fieldErrors(): Map<CreateField, String> {
    val apiException = this as? ApiException ?: return emptyMap()
    return apiException.fieldErrors.mapNotNull { error ->
        val field = when (error.field) {
            "title" -> CreateField.TITLE
            "description" -> CreateField.DESCRIPTION
            "price" -> CreateField.PRICE
            "category_id" -> CreateField.CATEGORY
            "custom_filters" -> CreateField.CUSTOM_FILTERS
            else -> null
        }
        field?.let { it to error.message }
    }.toMap()
}

private fun String?.orGenericError(): String =
    takeUnless { it.isNullOrBlank() } ?: "Something went wrong. Please try again."
