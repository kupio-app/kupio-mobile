package kupio.mobile.features.listings.presentation.create

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
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.form.ListingFormController
import kupio.mobile.features.listings.presentation.form.toCreateError
import kupio.mobile.features.listings.presentation.form.toUpload
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService

class CreateViewModel(
    private val listingsRepository: ListingsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {

    private val _state = MutableStateFlow(CreateState())
    val state: StateFlow<CreateState> = _state.asStateFlow()

    private val effectChannel = Channel<CreateEffect>(Channel.BUFFERED)
    val effects: Flow<CreateEffect> = effectChannel.receiveAsFlow()

    private var pendingCreatedListingId: String? = null
    private var uploadedImagesListingId: String? = null
    private val formController = ListingFormController(
        categoriesRepository = categoriesRepository,
        scope = viewModelScope,
        currentState = { _state.value },
        updateState = { transform -> _state.update(transform) },
        onFormChanged = ::resetPendingSubmission,
    )

    init {
        formController.loadCategories()
    }

    fun onIntent(intent: CreateIntent) {
        if (_state.value.isSubmitting && intent != CreateIntent.Back) return

        when (intent) {
            CreateIntent.ImageLimitReached -> _state.update {
                it.copy(imageWarning = CreateError.ImageLimitReached(MaxListingImages))
            }
            is CreateIntent.ImagesSelected -> addImages(intent.images)
            is CreateIntent.RemoveImage -> {
                resetPendingSubmission()
                _state.update {
                    it.copy(
                        images = it.images.filterNot { image -> image.id == intent.id },
                        imageWarning = null,
                    )
                }
            }
            is CreateIntent.MoveImage -> moveImage(intent.fromIndex, intent.toIndex)
            is CreateIntent.TitleChanged,
            is CreateIntent.DescriptionChanged,
            is CreateIntent.PriceChanged,
            is CreateIntent.CurrencyChanged,
            is CreateIntent.CategorySelected,
            CreateIntent.CategoryPickerReset,
            CreateIntent.CategoryPickerBack,
            CreateIntent.RetrySubcategories,
            is CreateIntent.FilterTextChanged,
            is CreateIntent.FilterBooleanChanged,
            CreateIntent.ToggleFree,
            CreateIntent.ToggleTradable,
            CreateIntent.RetryCategories,
            CreateIntent.RetryFilters,
            -> formController.handle(intent)
            CreateIntent.SaveDraft -> submit(activate = false)
            CreateIntent.Publish -> submit(activate = true)
            CreateIntent.Back -> viewModelScope.launch { effectChannel.send(CreateEffect.NavigateBack) }
        }
    }

    private fun addImages(images: List<SelectedListingImage>) {
        if (images.isEmpty()) return
        resetPendingSubmission()
        _state.update { state ->
            val existingIds = state.images.map { it.id }.toSet()
            val supportedImages = images.filter { isSupportedListingImageMimeType(it.mimeType) }
            val newImages = supportedImages.filterNot { it.id in existingIds }
            val remainingSlots = MaxListingImages - state.images.size
            val merged = state.images + newImages.take(remainingSlots.coerceAtLeast(0))
            val hitLimit = remainingSlots <= 0 || newImages.size > remainingSlots
            val hasUnsupportedImages = supportedImages.size != images.size
            val warning = when {
                hasUnsupportedImages -> CreateError.UnsupportedImage
                hitLimit -> CreateError.ImageLimitReached(MaxListingImages)
                else -> null
            }
            if (hasUnsupportedImages) analytics.logEvent("image_error", mapOf("reason" to "unsupported"))
            if (hitLimit) analytics.logEvent("image_error", mapOf("reason" to "limit_reached"))

            state.copy(
                images = merged,
                imageWarning = warning,
            )
        }
    }

    private fun moveImage(fromIndex: Int, toIndex: Int) {
        resetPendingSubmission()
        _state.update { state ->
            val images = state.images.move(fromIndex, toIndex) ?: return@update state
            state.copy(images = images, imageWarning = null)
        }
    }

    private fun submit(activate: Boolean) {
        if (_state.value.isSubmitting) return

        if (_state.value.images.any { !isSupportedListingImageMimeType(it.mimeType) }) {
            _state.update {
                it.copy(
                    imageWarning = CreateError.UnsupportedImage,
                    submitError = CreateError.UnsupportedImage,
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
                val listingId = pendingCreatedListingId
                    ?: listingsRepository.createListing(listing).id.also { pendingCreatedListingId = it }
                if (uploadedImagesListingId != listingId) {
                    listingsRepository.uploadListingImages(
                        listingId = listingId,
                        images = images.map { it.toUpload() },
                    )
                    uploadedImagesListingId = listingId
                }
                if (activate) {
                    listingsRepository.updateListingStatus(
                        listingId = listingId,
                        status = ListingStatus.ACTIVE,
                    )
                }
            }.onSuccess {
                resetPendingSubmission()
                _state.update {
                    CreateState(
                        categories = it.categories,
                        isLoadingCategories = it.isLoadingCategories,
                        categoriesError = it.categoriesError,
                    )
                }
                analytics.logEvent("create_listing", mapOf(
                    "status" to if (activate) "published" else "draft",
                    "image_count" to images.size.toString(),
                ))
                effectChannel.send(CreateEffect.NavigateBack)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        fieldErrors = throwable.fieldErrors(),
                        submitError = throwable.message.toCreateError(),
                    )
                }
                analytics.recordException(throwable, mapOf("screen" to "create_listing"))
            }
        }
    }

    private fun resetPendingSubmission() {
        pendingCreatedListingId = null
        uploadedImagesListingId = null
    }
}

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
