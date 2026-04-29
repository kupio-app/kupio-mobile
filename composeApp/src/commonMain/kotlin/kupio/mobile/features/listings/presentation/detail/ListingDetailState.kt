package kupio.mobile.features.listings.presentation.detail

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing

data class ListingDetailState(
    val listing: Listing? = null,
    val seller: ListingSellerUi? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isMessageDialogVisible: Boolean = false,
    val messageDraft: String = "",
    val isSendingMessage: Boolean = false,
    val messageError: String? = null,
) : UiState

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
    data object Back : ListingDetailIntent
    data object OpenMessageDialog : ListingDetailIntent
    data object CloseMessageDialog : ListingDetailIntent
    data class MessageChanged(val value: String) : ListingDetailIntent
    data object SendMessage : ListingDetailIntent
    data object CallSeller : ListingDetailIntent
    data object ReportListing : ListingDetailIntent
    data object OpenSellerProfile : ListingDetailIntent
}

sealed interface ListingDetailEffect : UiEffect {
    data object NavigateBack : ListingDetailEffect
    data class OpenChat(val conversationId: String) : ListingDetailEffect
}
