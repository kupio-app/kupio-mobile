package kupio.mobile.features.promotions.presentation.promote

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.promotions.domain.model.ListingPromotion
import kupio.mobile.features.promotions.domain.model.PromotionPacket

data class PromoteListingState(
    val listing: Listing? = null,
    val packets: List<PromotionPacket> = emptyList(),
    val selectedPacketId: Int? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val createdPromotion: ListingPromotion? = null,
) : UiState {
    val selectedPacket: PromotionPacket?
        get() = packets.firstOrNull { it.id == selectedPacketId }

    val canSubmit: Boolean
        get() = selectedPacket != null && !isSubmitting && createdPromotion == null
}

sealed interface PromoteListingIntent : UiAction {
    data object BackClicked : PromoteListingIntent
    data object RetryLoad : PromoteListingIntent
    data class PacketSelected(val packetId: Int) : PromoteListingIntent
    data object PromoteClicked : PromoteListingIntent
    data object DoneClicked : PromoteListingIntent
}

sealed interface PromoteListingEffect : UiEffect {
    data object NavigateBack : PromoteListingEffect
}
