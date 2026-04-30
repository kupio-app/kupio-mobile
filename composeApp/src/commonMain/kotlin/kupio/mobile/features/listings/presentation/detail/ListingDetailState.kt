package kupio.mobile.features.listings.presentation.detail

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingStatus

data class ListingDetailState(
    val listing: Listing? = null,
    val ownerMetadata: ListingOwnerMetadataUi? = null,
    val seller: ListingSellerUi? = null,
    val isOwnListing: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isMessageSheetVisible: Boolean = false,
    val messageDraft: String = "",
    val isSendingMessage: Boolean = false,
    val messageError: String? = null,
    val isUpdatingStatus: Boolean = false,
    val statusError: String? = null,
    val statusChangeTarget: ListingStatus? = null,
) : UiState

data class ListingOwnerMetadataUi(
    val status: ListingStatus,
    val seenCount: Int,
    val favouritesCount: Int,
    val chatsCount: Int,
    val isPromoted: Boolean,
)

data class ListingSellerUi(
    val displayName: String,
    val phone: String?,
    val isCallsDisabled: Boolean,
) {
    val initials: String = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { displayName.take(2).uppercase() }
}

sealed interface ListingDetailIntent : UiAction {
    data object Retry : ListingDetailIntent
    data object RefreshListing : ListingDetailIntent
    data object Back : ListingDetailIntent
    data object OpenMessageSheet : ListingDetailIntent
    data object CloseMessageSheet : ListingDetailIntent
    data class MessageChanged(val value: String) : ListingDetailIntent
    data object SendMessage : ListingDetailIntent
    data object CallSeller : ListingDetailIntent
    data object EditListing : ListingDetailIntent
    data object PromoteListing : ListingDetailIntent
    data object ToggleOwnerStatus : ListingDetailIntent
    data object ConfirmOwnerStatusChange : ListingDetailIntent
    data object DismissOwnerStatusChange : ListingDetailIntent
    data object ReportListing : ListingDetailIntent
    data object OpenSellerProfile : ListingDetailIntent
}

sealed interface ListingDetailEffect : UiEffect {
    data object NavigateBack : ListingDetailEffect
    data class OpenChat(val conversationId: String) : ListingDetailEffect
    data class OpenEdit(val listingId: String) : ListingDetailEffect
    data class NavigateToReport(
        val listingId: String,
        val listingTitle: String,
        val listingImageUrl: String,
        val listingPriceFormatted: String,
    ) : ListingDetailEffect
}
