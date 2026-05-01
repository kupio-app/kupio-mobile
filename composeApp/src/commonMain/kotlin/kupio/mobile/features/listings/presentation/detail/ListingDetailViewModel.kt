package kupio.mobile.features.listings.presentation.detail

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
import kupio.mobile.core.platform.PhoneDialer
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.chats.domain.repository.ConversationsRefresher
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.repository.MessagesRepository
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.repository.MeRepository
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import kupio.mobile.features.saved.domain.ToggleFavouriteUseCase
import kupio.mobile.core.analytics.AnalyticsService

class ListingDetailViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
    private val meRepository: MeRepository,
    private val chatsRepository: ChatsRepository,
    private val messagesRepository: MessagesRepository,
    private val conversationsRefresher: ConversationsRefresher,
    private val phoneDialer: PhoneDialer,
    private val sessionManager: AuthSessionManager,
    private val favouritesRepository: FavouritesRepository,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val analytics: AnalyticsService,
) : ViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    private val effectChannel = Channel<ListingDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ListingDetailEffect> = effectChannel.receiveAsFlow()

    init { load() }

    fun onIntent(intent: ListingDetailIntent) {
        when (intent) {
            ListingDetailIntent.Retry -> load()
            ListingDetailIntent.RefreshListing -> load(refresh = true)
            ListingDetailIntent.Back -> viewModelScope.launch {
                effectChannel.send(ListingDetailEffect.NavigateBack)
            }
            ListingDetailIntent.OpenMessageSheet -> _state.update {
                it.copy(isMessageSheetVisible = true, messageError = null)
            }
            ListingDetailIntent.CloseMessageSheet -> _state.update {
                it.copy(isMessageSheetVisible = false, messageError = null, messageDraft = "")
            }
            is ListingDetailIntent.MessageChanged -> _state.update {
                it.copy(messageDraft = intent.value, messageError = null)
            }
            ListingDetailIntent.SendMessage -> sendMessage()
            ListingDetailIntent.CallSeller -> callSeller()
            ListingDetailIntent.ToggleFavourite -> toggleFavourite()
            ListingDetailIntent.ToggleOwnerStatus -> prepareOwnerStatusChange()
            ListingDetailIntent.ConfirmOwnerStatusChange -> confirmOwnerStatusChange()
            ListingDetailIntent.DismissOwnerStatusChange -> _state.update { it.copy(statusChangeTarget = null) }
            ListingDetailIntent.EditListing -> openEdit()
            ListingDetailIntent.PromoteListing,
            ListingDetailIntent.OpenSellerProfile,
            -> Unit
            ListingDetailIntent.ReportListing -> reportListing()
        }
    }

    private fun load(refresh: Boolean = false) {
        _state.update {
            if (refresh) {
                it.copy(isRefreshing = true, errorMessage = null)
            } else {
                it.copy(isLoading = true, errorMessage = null)
            }
        }
        viewModelScope.launch {
            runCatching {
                val listing = listingsRepository.getListingDetail(listingId)
                val currentUserId = sessionManager.currentUserId()
                val isOwnListing = currentUserId.isNotBlank() && listing.userId == currentUserId
                val ownerMetadata = if (isOwnListing) {
                    runCatching { meRepository.getMyListing(listing.id)?.toOwnerMetadataUi() }
                        .onFailure { if (it is CancellationException) throw it }
                        .getOrNull()
                } else {
                    null
                }
                val isFavourited = if (!isOwnListing) {
                    runCatching { favouritesRepository.getFavouriteIds().contains(listing.id) }
                        .onFailure { if (it is CancellationException) throw it }
                        .getOrDefault(false)
                } else {
                    false
                }
                LoadedListing(
                    listing = listing,
                    isOwnListing = isOwnListing,
                    ownerMetadata = ownerMetadata,
                    isFavourited = isFavourited,
                )
            }
                .onSuccess { loaded ->
                    _state.update {
                        it.copy(
                            listing = loaded.listing,
                            ownerMetadata = loaded.ownerMetadata,
                            seller = loaded.listing.toSellerUi(),
                            isOwnListing = loaded.isOwnListing,
                            isFavourited = loaded.isFavourited,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                    analytics.logEvent("view_item", mapOf("item_id" to loaded.listing.id))
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = if (refresh && it.listing != null) {
                                null
                            } else {
                                t.message.orEmpty()
                            },
                        )
                    }
                    analytics.recordException(t, mapOf("screen" to "listing_detail", "listing_id" to listingId))
                }
        }
    }

    private fun sendMessage() {
        val listing = _state.value.listing ?: return
        if (_state.value.isOwnListing) return
        val message = _state.value.messageDraft.trim()
        if (message.isBlank()) {
            _state.update { it.copy(messageError = "") }
            return
        }
        if (_state.value.isSendingMessage) return

        _state.update { it.copy(isSendingMessage = true, messageError = null) }
        viewModelScope.launch {
            runCatching {
                val existing = chatsRepository
                    .listConversations(ChatRole.BUYING)
                    .firstOrNull { conversation ->
                        conversation.listingId == listing.id && conversation.sellerId == listing.userId
                    }
                if (existing != null) {
                    messagesRepository.sendMessage(existing.id, message)
                    existing
                } else {
                    chatsRepository.startConversation(listing.id, message)
                }
            }.onSuccess { conversation ->
                runCatching { conversationsRefresher.refresh() }
                _state.update {
                    it.copy(
                        isSendingMessage = false,
                        isMessageSheetVisible = false,
                        messageDraft = "",
                    )
                }
                analytics.logEvent("contact_seller", mapOf("item_id" to listing.id, "method" to "message"))
                effectChannel.send(ListingDetailEffect.OpenChat(conversation.id))
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isSendingMessage = false,
                        messageError = t.message.orEmpty(),
                    )
                }
                analytics.recordException(t, mapOf("action" to "send_message"))
            }
        }
    }

    private fun toggleFavourite() {
        val listing = _state.value.listing ?: return
        if (_state.value.isOwnListing || _state.value.isTogglingFavourite) return
        val adding = !_state.value.isFavourited
        _state.update { it.copy(isFavourited = adding, isTogglingFavourite = true) }
        val event = if (adding) "add_to_favourites" else "remove_from_favourites"
        analytics.logEvent(event, mapOf("item_id" to listing.id))
        viewModelScope.launch {
            toggleFavouriteUseCase(listing.id, adding).onFailure {
                _state.update { it.copy(isFavourited = !adding) }
            }
            _state.update { it.copy(isTogglingFavourite = false) }
        }
    }

    private fun callSeller() {
        val listing = _state.value.listing ?: return
        if (_state.value.isOwnListing) return
        val seller = _state.value.seller ?: return
        if (seller.isCallsDisabled) return
        val phone = seller.phone?.takeIf { it.isNotBlank() } ?: return
        analytics.logEvent("contact_seller", mapOf("item_id" to listing.id, "method" to "phone"))
        phoneDialer.openDialer(phone)
    }

    private fun prepareOwnerStatusChange() {
        val listing = _state.value.listing ?: return
        if (!_state.value.isOwnListing || _state.value.isUpdatingStatus) return
        val currentStatus = _state.value.ownerMetadata?.status ?: listing.status
        val targetStatus = currentStatus.nextToggleStatus() ?: return
        _state.update { it.copy(statusChangeTarget = targetStatus, statusError = null) }
    }

    private fun confirmOwnerStatusChange() {
        val listing = _state.value.listing ?: return
        val targetStatus = _state.value.statusChangeTarget ?: return
        if (!_state.value.isOwnListing || _state.value.isUpdatingStatus) return

        _state.update { it.copy(isUpdatingStatus = true, statusError = null, statusChangeTarget = null) }
        viewModelScope.launch {
            runCatching {
                listingsRepository.updateListingStatus(listing.id, targetStatus)
            }.onSuccess { updated ->
                _state.update {
                    it.copy(
                        listing = updated,
                        ownerMetadata = it.ownerMetadata?.copy(status = targetStatus),
                        isUpdatingStatus = false,
                        statusError = null,
                    )
                }
                analytics.logEvent("update_listing_status", mapOf("item_id" to listing.id, "new_status" to targetStatus.name))
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isUpdatingStatus = false,
                        statusError = t.message.orEmpty(),
                    )
                }
            }
        }
    }

    private fun openEdit() {
        val listing = _state.value.listing ?: return
        if (!_state.value.isOwnListing) return
        viewModelScope.launch {
            effectChannel.send(ListingDetailEffect.OpenEdit(listing.id))
        }
    }

    private fun reportListing() {
        val listing = _state.value.listing ?: return
        if (_state.value.isOwnListing) return
        viewModelScope.launch {
            effectChannel.send(
                ListingDetailEffect.NavigateToReport(
                    listingId = listing.id,
                    listingTitle = listing.title,
                    listingImageUrl = listing.primaryImageUrl.orEmpty(),
                    listingPriceFormatted = listing.formatPrice(),
                )
            )
        }
    }

    private fun Listing.toSellerUi(): ListingSellerUi {
        return ListingSellerUi(
            displayName = contactName.orEmpty(),
            phone = phone,
            isCallsDisabled = isCallsDisabled,
        )
    }
}

private data class LoadedListing(
    val listing: Listing,
    val isOwnListing: Boolean,
    val ownerMetadata: ListingOwnerMetadataUi?,
    val isFavourited: Boolean,
)

private fun OwnedListing.toOwnerMetadataUi(): ListingOwnerMetadataUi = ListingOwnerMetadataUi(
    status = status.toListingStatus(),
    seenCount = seenCount,
    favouritesCount = favouritesCount,
    chatsCount = chatsCount,
    isPromoted = isPromoted,
)

private fun OwnedListingStatus.toListingStatus(): ListingStatus = when (this) {
    OwnedListingStatus.ACTIVE -> ListingStatus.ACTIVE
    OwnedListingStatus.INACTIVE -> ListingStatus.INACTIVE
    OwnedListingStatus.DRAFT -> ListingStatus.DRAFT
    OwnedListingStatus.PLANNED -> ListingStatus.PLANNED
    OwnedListingStatus.SOLD -> ListingStatus.SOLD
}

private fun ListingStatus.nextToggleStatus(): ListingStatus? = when (this) {
    ListingStatus.ACTIVE -> ListingStatus.INACTIVE
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    -> ListingStatus.ACTIVE
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> null
}
