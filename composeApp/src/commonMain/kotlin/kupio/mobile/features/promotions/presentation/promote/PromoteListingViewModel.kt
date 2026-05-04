package kupio.mobile.features.promotions.presentation.promote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.promotions.domain.model.PromotionPacket
import kupio.mobile.features.promotions.domain.repository.PromotionsRepository

class PromoteListingViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
    private val promotionsRepository: PromotionsRepository,
    private val sessionManager: AuthSessionManager,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {

    private val _state = MutableStateFlow(PromoteListingState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<PromoteListingEffect>(Channel.BUFFERED)
    val effects: Flow<PromoteListingEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: PromoteListingIntent) {
        when (intent) {
            PromoteListingIntent.BackClicked,
            PromoteListingIntent.DoneClicked,
            -> viewModelScope.launch { effectChannel.send(PromoteListingEffect.NavigateBack) }
            PromoteListingIntent.RetryLoad -> load()
            is PromoteListingIntent.PacketSelected -> _state.update {
                if (it.createdPromotion != null || it.isSubmitting) {
                    it
                } else {
                    it.copy(selectedPacketId = intent.packetId, submitError = null)
                }
            }
            PromoteListingIntent.PromoteClicked -> promote()
        }
    }

    private fun load() {
        _state.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                submitError = null,
                createdPromotion = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val listing = async { listingsRepository.getListing(listingId) }
                    val packets = async { promotionsRepository.getPackets() }
                    LoadedPromotionData(
                        listing = listing.await(),
                        packets = packets.await().filter { it.isActive },
                    )
                }
            }.onSuccess { loaded ->
                _state.update {
                    val selectedPacketId = it.selectedPacketId
                        ?.takeIf { id -> loaded.packets.any { packet -> packet.id == id } }
                        ?: loaded.packets.firstOrNull()?.id

                    it.copy(
                        listing = loaded.listing,
                        packets = loaded.packets,
                        selectedPacketId = selectedPacketId,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (throwable is AuthSessionExpiredException) {
                    sessionManager.expireSession()
                    _state.update { it.copy(isLoading = false) }
                    return@onFailure
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.readMessage(),
                    )
                }
                analytics.recordException(
                    throwable,
                    mapOf("screen" to "promote_listing", "listing_id" to listingId),
                )
            }
        }
    }

    private fun promote() {
        val packet = _state.value.selectedPacket ?: return
        if (!_state.value.canSubmit) return

        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            runCatching {
                promotionsRepository.promoteListing(
                    listingId = listingId,
                    packetId = packet.id,
                )
            }.onSuccess { promotion ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = null,
                        createdPromotion = promotion,
                    )
                }
                analytics.logEvent(
                    "promote_listing",
                    mapOf(
                        "item_id" to listingId,
                        "promotion_packet_id" to packet.id.toString(),
                        "promotion_type" to packet.type.name,
                    ),
                )
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (throwable is AuthSessionExpiredException) {
                    sessionManager.expireSession()
                    _state.update { it.copy(isSubmitting = false) }
                    return@onFailure
                }
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = throwable.readMessage(),
                    )
                }
                analytics.recordException(
                    throwable,
                    mapOf("action" to "promote_listing", "listing_id" to listingId),
                )
            }
        }
    }
}

private data class LoadedPromotionData(
    val listing: Listing,
    val packets: List<PromotionPacket>,
)

private fun Throwable.readMessage(): String? = message?.takeIf { it.isNotBlank() }
