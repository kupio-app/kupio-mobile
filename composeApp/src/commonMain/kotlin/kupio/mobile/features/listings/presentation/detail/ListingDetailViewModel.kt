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
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.repository.MeRepository

class ListingDetailViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
    private val meRepository: MeRepository,
    private val chatsRepository: ChatsRepository,
    private val messagesRepository: MessagesRepository,
    private val conversationsRefresher: ConversationsRefresher,
    private val phoneDialer: PhoneDialer,
    private val sessionManager: AuthSessionManager,
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
            ListingDetailIntent.ToggleOwnerStatus -> prepareOwnerStatusChange()
            ListingDetailIntent.ConfirmOwnerStatusChange -> confirmOwnerStatusChange()
            ListingDetailIntent.DismissOwnerStatusChange -> _state.update { it.copy(statusChangeTarget = null) }
            ListingDetailIntent.EditListing,
            ListingDetailIntent.PromoteListing,
            ListingDetailIntent.ReportListing,
            ListingDetailIntent.OpenSellerProfile,
            -> Unit
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
                    runCatching { meRepository.getMyListing(listing.id)?.toOwnerMetadataUi() }.getOrNull()
                } else {
                    null
                }
                LoadedListing(
                    listing = listing,
                    isOwnListing = isOwnListing,
                    ownerMetadata = ownerMetadata,
                )
            }
                .onSuccess { loaded ->
                    _state.update {
                        it.copy(
                            listing = loaded.listing,
                            ownerMetadata = loaded.ownerMetadata,
                            seller = loaded.listing.toSellerUi(),
                            isOwnListing = loaded.isOwnListing,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
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
                }
        }
    }

    private fun sendMessage() {
        val listing = _state.value.listing ?: return
        if (_state.value.isOwnListing) return
        val message = _state.value.messageDraft.trim()
        if (message.isBlank()) {
            _state.update { it.copy(messageError = "Enter a message.") }
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
                effectChannel.send(ListingDetailEffect.OpenChat(conversation.id))
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isSendingMessage = false,
                        messageError = t.message ?: "Could not send message.",
                    )
                }
            }
        }
    }

    private fun callSeller() {
        if (_state.value.isOwnListing) return
        val seller = _state.value.seller ?: return
        if (seller.isCallsDisabled) return
        val phone = seller.phone?.takeIf { it.isNotBlank() } ?: return
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
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isUpdatingStatus = false,
                        statusError = t.message ?: "Could not update listing status.",
                    )
                }
            }
        }
    }

    private fun Listing.toSellerUi(): ListingSellerUi {
        val label = contactName?.takeIf { it.isNotBlank() } ?: "Seller"
        return ListingSellerUi(
            displayName = label,
            phone = phone,
            isCallsDisabled = isCallsDisabled,
        )
    }
}

private data class LoadedListing(
    val listing: Listing,
    val isOwnListing: Boolean,
    val ownerMetadata: ListingOwnerMetadataUi?,
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
