package kupio.mobile.features.me.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.offline.OfflineMutationStore
import kupio.mobile.core.offline.OfflineSyncManager
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.me.data.remote.MeApi
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats
import kupio.mobile.features.me.domain.repository.MeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MeRepositoryImpl(
    private val meApi: MeApi,
    private val apiClient: AuthenticatedApiClient,
    private val offlineStore: OfflineMutationStore,
    private val offlineSyncManager: OfflineSyncManager,
    private val sessionManager: AuthSessionManager,
    private val appScope: CoroutineScope,
) : MeRepository {

    override suspend fun getStats(): UserListingStats =
        apiClient.request { meApi.getStats(it) }

    override suspend fun getMyListings(): List<OwnedListing> =
        runCatching {
            apiClient.request { meApi.getMyListings(it) }
                .also { offlineStore.cacheOwnedListings(it, sessionManager.currentUserId()) }
        }.getOrElse {
            offlineStore.getOwnedListings(sessionManager.currentUserId())
        }

    override suspend fun getMyListing(listingId: String): OwnedListing? {
        offlineStore.getOwnedListings(sessionManager.currentUserId())
            .firstOrNull { it.id == listingId }
            ?.let { return it }

        var cursor: String? = null
        do {
            val page = apiClient.request {
                meApi.getMyListingsPage(
                    authorize = it,
                    limit = OWNER_LISTINGS_LOOKUP_PAGE_SIZE,
                    cursor = cursor,
                )
            }
            page.listings.firstOrNull { it.id == listingId }?.let { return it }
            cursor = page.nextCursor
        } while (cursor != null)
        return null
    }

    override suspend fun updateListingStatus(listingId: String, status: OwnedListingStatus) {
        offlineStore.queueListingStatus(
            listingId = listingId,
            status = status.toListingStatus(),
            currentListing = offlineStore.getListing(listingId),
        )
        appScope.launch {
            runCatching { offlineSyncManager.syncPending() }
        }
    }

    private companion object {
        const val OWNER_LISTINGS_LOOKUP_PAGE_SIZE = 100
    }
}

private fun OwnedListingStatus.toListingStatus(): ListingStatus = when (this) {
    OwnedListingStatus.ACTIVE -> ListingStatus.ACTIVE
    OwnedListingStatus.INACTIVE -> ListingStatus.INACTIVE
    OwnedListingStatus.DRAFT -> ListingStatus.DRAFT
    OwnedListingStatus.PLANNED -> ListingStatus.PLANNED
    OwnedListingStatus.SOLD -> ListingStatus.SOLD
}
