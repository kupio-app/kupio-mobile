package kupio.mobile.features.listings.presentation.edit

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState
import kupio.mobile.features.listings.presentation.create.SelectedListingImage
import kupio.mobile.features.listings.presentation.form.ListingFormImage
import kupio.mobile.features.listings.presentation.form.RemoteListingImage

data class EditListingState(
    val listing: Listing? = null,
    val form: CreateState = CreateState(),
    val images: List<ListingFormImage> = emptyList(),
    val originalImages: List<RemoteListingImage> = emptyList(),
    val isLoadingListing: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: CreateError? = null,
    val imageWarning: CreateError? = null,
    val submitError: CreateError? = null,
) : UiState {
    val canSave: Boolean
        get() = !isLoadingListing &&
            !isSaving &&
            form.canSubmit &&
            form.filtersError == null
}

sealed interface EditListingIntent : UiAction {
    data object Back : EditListingIntent
    data object Retry : EditListingIntent
    data object Save : EditListingIntent
    data object ImageLimitReached : EditListingIntent
    data class ImagesSelected(val images: List<SelectedListingImage>) : EditListingIntent
    data class RemoveImage(val id: String) : EditListingIntent
    data class MoveImage(val fromIndex: Int, val toIndex: Int) : EditListingIntent
    data class FormIntent(val intent: CreateIntent) : EditListingIntent
}

sealed interface EditListingEffect : UiEffect {
    data object NavigateBack : EditListingEffect
    data class OpenListing(val id: String) : EditListingEffect
}
