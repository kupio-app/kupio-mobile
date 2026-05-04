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
            MyListingsIntent.RetryLoad -> loadListings()
            MyListingsIntent.RefreshListings -> loadListings(refresh = true)
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

    private fun loadListings(refresh: Boolean = false) {
        _state.update {
            if (refresh) {
                it.copy(isRefreshing = true, errorMessage = null)
            } else {
                it.copy(isLoading = true, errorMessage = null)
            }
        }
        viewModelScope.launch {
            runCatching { meRepository.getMyListings() }
                .onSuccess { listings ->
                    val active = listings.count { it.status == OwnedListingStatus.ACTIVE }
                    val inactive = listings.count { it.status == OwnedListingStatus.INACTIVE }
                    _state.update {
                        it.copy(
                            listings = listings,
                            activeCount = active,
                            inactiveCount = inactive,
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
                            errorMessage = t.message,
                        )
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
