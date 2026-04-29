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
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ListingDetailViewModel(
    private val listingId: String,
    private val listingsRepository: ListingsRepository,
    private val chatsRepository: ChatsRepository,
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
            ListingDetailIntent.ToggleOwnerStatus -> toggleOwnerStatus()
            ListingDetailIntent.EditListing,
            ListingDetailIntent.PromoteListing,
            ListingDetailIntent.ReportListing,
            ListingDetailIntent.OpenSellerProfile,
            -> Unit
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val listing = listingsRepository.getListingDetail(listingId)
                val currentUserId = sessionManager.currentUserId()
                listing to (currentUserId.isNotBlank() && listing.userId == currentUserId)
            }
                .onSuccess { (listing, isOwnListing) ->
                    _state.update {
                        it.copy(
                            listing = listing,
                            seller = listing.toSellerUi(),
                            isOwnListing = isOwnListing,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message.orEmpty()) }
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
                chatsRepository.startConversation(listing.id, message)
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

    private fun toggleOwnerStatus() {
        val listing = _state.value.listing ?: return
        if (!_state.value.isOwnListing || _state.value.isUpdatingStatus) return
        val targetStatus = listing.status.nextToggleStatus() ?: return

        _state.update { it.copy(isUpdatingStatus = true, statusError = null) }
        viewModelScope.launch {
            runCatching {
                listingsRepository.updateListingStatus(listing.id, targetStatus)
            }.onSuccess { updated ->
                _state.update {
                    it.copy(
                        listing = updated,
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

private fun ListingStatus.nextToggleStatus(): ListingStatus? = when (this) {
    ListingStatus.ACTIVE -> ListingStatus.INACTIVE
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    -> ListingStatus.ACTIVE
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> null
}
