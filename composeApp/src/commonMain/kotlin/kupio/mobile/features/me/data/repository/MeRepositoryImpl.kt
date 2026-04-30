package kupio.mobile.features.me.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.me.data.remote.MeApi
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats
import kupio.mobile.features.me.domain.repository.MeRepository

class MeRepositoryImpl(
    private val meApi: MeApi,
    private val apiClient: AuthenticatedApiClient,
) : MeRepository {

    override suspend fun getStats(): UserListingStats =
        apiClient.request { meApi.getStats(it) }

    override suspend fun getMyListings(): List<OwnedListing> =
        apiClient.request { meApi.getMyListings(it) }

    override suspend fun getMyListing(listingId: String): OwnedListing? {
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
        apiClient.request { meApi.updateListingStatus(it, listingId, status) }
    }

    private companion object {
        const val OWNER_LISTINGS_LOOKUP_PAGE_SIZE = 100
    }
}
