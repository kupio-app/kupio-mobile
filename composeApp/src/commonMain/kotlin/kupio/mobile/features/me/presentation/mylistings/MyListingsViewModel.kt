package kupio.mobile.features.me.presentation.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.repository.MeRepository
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService

class MyListingsViewModel(
    private val meRepository: MeRepository,
    private val sessionManager: AuthSessionManager,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {
    private val _state = MutableStateFlow(MyListingsState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<MyListingsEffect>(Channel.BUFFERED)
    val effects: Flow<MyListingsEffect> = effectChannel.receiveAsFlow()

    init {
        loadListings()
    }

    fun onIntent(intent: MyListingsIntent) {
        when (intent) {
            is MyListingsIntent.FilterSelected -> _state.update { it.copy(filter = intent.filter) }
            MyListingsIntent.BackClicked -> viewModelScope.launch {
                effectChannel.send(MyListingsEffect.NavigateBack)
            }
            is MyListingsIntent.EditListing -> viewModelScope.launch {
                effectChannel.send(MyListingsEffect.EditListing(intent.id))
            }
            is MyListingsIntent.BumpUp -> Unit
            is MyListingsIntent.Promote -> viewModelScope.launch {
                effectChannel.send(MyListingsEffect.PromoteListing(intent.id))
            }
            is MyListingsIntent.OpenListing -> viewModelScope.launch {
                effectChannel.send(MyListingsEffect.OpenListing(intent.id))
            }
            is MyListingsIntent.ToggleActiveClicked -> prepareStatusChange(intent.id)
            MyListingsIntent.ConfirmStatusChange -> confirmStatusChange()
            MyListingsIntent.DismissStatusChange -> _state.update { it.copy(statusChangeConfirmation = null) }
            MyListingsIntent.RetryLoad -> loadListings(reset = true)
            MyListingsIntent.RefreshListings -> loadListings(reset = true, isRefresh = true)
            MyListingsIntent.LoadMore -> {
                if (!_state.value.isLoadingMore && _state.value.hasMore) loadListings(reset = false)
            }
        }
    }

    private fun prepareStatusChange(listingId: String) {
        if (_state.value.updatingListingId != null) return
        val listing = _state.value.listings.firstOrNull { it.id == listingId } ?: return
        val targetStatus = listing.status.nextToggleStatus() ?: return
        _state.update {
            it.copy(
                statusChangeConfirmation = StatusChangeConfirmation(
                    listingId = listingId,
                    targetStatus = targetStatus,
                ),
            )
        }
    }

    private fun confirmStatusChange() {
        val confirmation = _state.value.statusChangeConfirmation ?: return
        if (_state.value.updatingListingId != null) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    updatingListingId = confirmation.listingId,
                    statusChangeConfirmation = null,
                    errorMessage = null,
                )
            }
            runCatching {
                meRepository.updateListingStatus(confirmation.listingId, confirmation.targetStatus)
            }.onSuccess {
                _state.update { state ->
                    val updatedListings = state.listings.map { listing ->
                        if (listing.id == confirmation.listingId) {
                            listing.copy(status = confirmation.targetStatus)
                        } else {
                            listing
                        }
                    }
                    val active = updatedListings.count { it.status == OwnedListingStatus.ACTIVE }
                    val inactive = updatedListings.count { it.status == OwnedListingStatus.INACTIVE }
                    state.copy(
                        listings = updatedListings,
                        activeCount = active,
                        inactiveCount = inactive,
                        updatingListingId = null,
                    )
                }
                analytics.logEvent("update_listing_status", mapOf("item_id" to confirmation.listingId, "new_status" to confirmation.targetStatus.name, "source" to "my_listings"))
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (throwable is AuthSessionExpiredException) {
                    sessionManager.expireSession()
                    _state.update { it.copy(updatingListingId = null) }
                    return@onFailure
                }
                _state.update {
                    it.copy(
                        updatingListingId = null,
                        errorMessage = throwable.message,
                    )
                }
            }
        }
    }

    private fun loadListings(reset: Boolean = true, isRefresh: Boolean = false) {
        val cursor = if (reset) null else _state.value.nextCursor
        _state.update {
            when {
                isRefresh -> it.copy(isRefreshing = true, errorMessage = null)
                reset -> it.copy(isLoading = true, errorMessage = null, nextCursor = null, hasMore = false)
                else -> it.copy(isLoadingMore = true)
            }
        }
        viewModelScope.launch {
            runCatching { meRepository.getMyListingsPage(cursor = cursor) }
                .onSuccess { page ->
                    _state.update { current ->
                        val accumulated = if (reset) page.listings else current.listings + page.listings
                        val active = accumulated.count { it.status == OwnedListingStatus.ACTIVE }
                        val inactive = accumulated.count { it.status == OwnedListingStatus.INACTIVE }
                        current.copy(
                            listings = accumulated,
                            activeCount = active,
                            inactiveCount = inactive,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            nextCursor = page.nextCursor,
                            hasMore = page.nextCursor != null,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    if (reset) {
                        _state.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = t.message) }
                    } else {
                        _state.update { it.copy(isLoadingMore = false) }
                    }
                    analytics.recordException(t, mapOf("screen" to "my_listings"))
                }
        }
    }
}

private fun OwnedListingStatus.nextToggleStatus(): OwnedListingStatus? = when (this) {
    OwnedListingStatus.ACTIVE -> OwnedListingStatus.INACTIVE
    OwnedListingStatus.INACTIVE -> OwnedListingStatus.ACTIVE
    OwnedListingStatus.DRAFT -> OwnedListingStatus.ACTIVE
    OwnedListingStatus.PLANNED,
    OwnedListingStatus.SOLD,
    -> null
}
