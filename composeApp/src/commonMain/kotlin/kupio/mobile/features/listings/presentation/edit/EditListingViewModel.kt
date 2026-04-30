package kupio.mobile.features.listings.presentation.edit

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
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.listings.data.image.MaxListingImages
import kupio.mobile.features.listings.data.image.isSupportedListingImageMimeType
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateField
import kupio.mobile.features.listings.presentation.create.CreateFilterInput
import kupio.mobile.features.listings.presentation.create.CreateState
import kupio.mobile.features.listings.presentation.create.SelectedListingImage
import kupio.mobile.features.listings.presentation.create.validateCreateListing
import kupio.mobile.features.listings.presentation.form.ListingFormController
import kupio.mobile.features.listings.presentation.form.LocalListingImage
import kupio.mobile.features.listings.presentation.form.RemoteListingImage
import kupio.mobile.features.listings.presentation.form.toCreateError
import kupio.mobile.features.listings.presentation.form.toUpload

class EditListingViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditListingState())
    val state: StateFlow<EditListingState> = _state.asStateFlow()

    private val effectChannel = Channel<EditListingEffect>(Channel.BUFFERED)
    val effects: Flow<EditListingEffect> = effectChannel.receiveAsFlow()

    private val formController = ListingFormController(
        categoriesRepository = categoriesRepository,
        scope = viewModelScope,
        currentState = { _state.value.form },
        updateState = { transform ->
            _state.update { state ->
                state.copy(
                    form = transform(state.form),
                    submitError = null,
                )
            }
        },
        onFormChanged = {
            _state.update { it.copy(submitError = null) }
        },
    )

    init {
        load()
    }

    fun onIntent(intent: EditListingIntent) {
        if (_state.value.isSaving && intent != EditListingIntent.Back) return

        when (intent) {
            EditListingIntent.Back -> viewModelScope.launch {
                effectChannel.send(EditListingEffect.NavigateBack)
            }
            EditListingIntent.Retry -> load()
            EditListingIntent.Save -> save()
            EditListingIntent.ImageLimitReached -> _state.update {
                it.copy(imageWarning = CreateError.ImageLimitReached(MaxListingImages))
            }
            is EditListingIntent.ImagesSelected -> addImages(intent.images)
            is EditListingIntent.RemoveImage -> _state.update {
                it.copy(
                    images = it.images.filterNot { image -> image.id == intent.id },
                    imageWarning = null,
                    submitError = null,
                )
            }
            is EditListingIntent.MoveImage -> moveImage(intent.fromIndex, intent.toIndex)
            is EditListingIntent.FormIntent -> formController.handle(intent.intent)
        }
    }

    private fun load() {
        formController.cancel()
        _state.update {
            it.copy(
                isLoadingListing = true,
                loadError = null,
                submitError = null,
                form = it.form.copy(
                    isLoadingCategories = true,
                    categoriesError = null,
                    isLoadingFilters = false,
                    filtersError = null,
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                val listing = listingsRepository.getListing(listingId)
                val categories = runCatching { categoriesRepository.getRootCategories(limit = 100) }
                val filters = runCatching { categoriesRepository.getCategoryFilters(listing.categoryId) }
                LoadedEditListing(
                    listing = listing,
                    categories = categories.getOrNull().orEmpty(),
                    categoriesError = categories.exceptionOrNull()?.let { it.message.toCreateError() },
                    filters = filters.getOrNull().orEmpty(),
                    filtersError = filters.exceptionOrNull()?.let { it.message.toCreateError() },
                )
            }.onSuccess { loaded ->
                val remoteImages = loaded.listing.toRemoteImages()
                _state.update {
                    it.copy(
                        listing = loaded.listing,
                        images = remoteImages,
                        originalImages = remoteImages,
                        isLoadingListing = false,
                        loadError = null,
                        form = loaded.toFormState(),
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(
                        isLoadingListing = false,
                        loadError = throwable.message.toCreateError(),
                        form = it.form.copy(isLoadingCategories = false, isLoadingFilters = false),
                    )
                }
            }
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
                    hasUnsupportedImages -> CreateError.UnsupportedImage
                    hitLimit -> CreateError.ImageLimitReached(MaxListingImages)
                    else -> null
                },
                submitError = null,
            )
        }
    }

    private fun moveImage(fromIndex: Int, toIndex: Int) {
        _state.update { state ->
            val images = state.images.move(fromIndex, toIndex) ?: return@update state
            state.copy(images = images, imageWarning = null, submitError = null)
        }
    }

    private fun save() {
        val state = _state.value
        if (state.isSaving || state.isLoadingListing) return

        val localImages = state.images.filterIsInstance<LocalListingImage>()
        if (localImages.any { !isSupportedListingImageMimeType(it.mimeType) }) {
            _state.update {
                it.copy(
                    imageWarning = CreateError.UnsupportedImage,
                    submitError = CreateError.UnsupportedImage,
                )
            }
            return
        }

        val validation = validateCreateListing(state.form)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    form = it.form.copy(
                        fieldErrors = validation.fieldErrors,
                        filterErrors = validation.filterErrors,
                    ),
                    submitError = null,
                )
            }
            return
        }

        val listingPayload = validation.listing ?: return
        val originalListing = state.listing ?: return
        _state.update {
            it.copy(
                isSaving = true,
                imageWarning = null,
                submitError = null,
                form = it.form.copy(
                    fieldErrors = emptyMap(),
                    filterErrors = emptyMap(),
                ),
            )
        }
        viewModelScope.launch {
            runCatching {
                listingsRepository.updateListing(
                    listingId = listingId,
                    listing = listingPayload,
                    phone = originalListing.phone,
                    contactName = originalListing.contactName,
                    isCallsDisabled = originalListing.isCallsDisabled,
                )
                val uploadedImages = listingsRepository.uploadListingImages(
                    listingId = listingId,
                    images = localImages.map { it.toUpload() },
                )
                if (uploadedImages.size != localImages.size) {
                    error("Could not upload all images.")
                }
                val uploadedImageIdsByLocalId = localImages
                    .zip(uploadedImages)
                    .associate { (localImage, uploadedImage) -> localImage.id to uploadedImage.id }

                val remainingRemoteImageIds = state.images
                    .filterIsInstance<RemoteListingImage>()
                    .map { it.imageId }
                    .toSet()
                val removedImageIds = state.originalImages
                    .map { it.imageId }
                    .filterNot { it in remainingRemoteImageIds }
                removedImageIds.forEach { imageId ->
                    listingsRepository.deleteListingImage(listingId, imageId)
                }

                val finalImageIds = state.images.mapNotNull { image ->
                    when (image) {
                        is RemoteListingImage -> image.imageId
                        is LocalListingImage -> uploadedImageIdsByLocalId[image.id]
                    }
                }
                if (finalImageIds.isNotEmpty()) {
                    listingsRepository.updateListingImagesOrder(listingId, finalImageIds)
                }
            }.onSuccess {
                _state.update { it.copy(isSaving = false) }
                effectChannel.send(EditListingEffect.OpenListing(listingId))
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(
                        isSaving = false,
                        form = it.form.copy(fieldErrors = throwable.fieldErrors()),
                        submitError = throwable.message.toCreateError(),
                    )
                }
            }
        }
    }
}

private data class LoadedEditListing(
    val listing: Listing,
    val categories: List<Category>,
    val categoriesError: CreateError?,
    val filters: List<FilterDefinition>,
    val filtersError: CreateError?,
)

private fun LoadedEditListing.toFormState(): CreateState {
    return CreateState(
        title = listing.title,
        description = listing.description,
        price = listing.price.toString(),
        currency = listing.currency,
        isFree = listing.isFree,
        isTradable = listing.isTradable,
        categories = categories,
        selectedCategoryId = listing.categoryId,
        selectedCategoryName = listing.categoryName,
        isLoadingCategories = false,
        categoriesError = categoriesError,
        filters = filters,
        filterValues = listing.customFilters.toFilterInputs(filters),
        isLoadingFilters = false,
        filtersError = filtersError,
    )
}

private fun Listing.toRemoteImages(): List<RemoteListingImage> =
    images.sortedBy { it.sortOrder }.map { image ->
        RemoteListingImage(
            id = "remote-${image.id}",
            imageId = image.id,
            imageUrl = image.url,
            sortOrder = image.sortOrder,
        )
    }

private fun Map<String, String>.toFilterInputs(
    filters: List<FilterDefinition>,
): Map<String, CreateFilterInput> = filters.mapNotNull { filter ->
    val value = this[filter.slug]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val input = when (filter.type) {
        FilterType.BOOLEAN -> CreateFilterInput.BooleanValue(value.toBooleanStrictOrNull())
        FilterType.TEXT,
        FilterType.NUMBER,
        FilterType.RANGE,
        FilterType.SELECT,
        -> CreateFilterInput.Text(value)
    }
    filter.slug to input
}.toMap()

private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T>? {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return null
    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

private fun Throwable.fieldErrors(): Map<CreateField, CreateError> {
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
        field?.let { it to CreateError.ServerMessage(error.message) }
    }.toMap()
}
