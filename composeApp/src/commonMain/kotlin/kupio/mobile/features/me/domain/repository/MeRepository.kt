package kupio.mobile.features.me.domain.repository

import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.OwnedListingsPage
import kupio.mobile.features.me.domain.model.UserListingStats

interface MeRepository {
    suspend fun getStats(): UserListingStats
    suspend fun getMyListings(): List<OwnedListing>
    suspend fun getMyListingsPage(cursor: String? = null, limit: Int = 20): OwnedListingsPage
    suspend fun getMyListing(listingId: String): OwnedListing?
    suspend fun updateListingStatus(listingId: String, status: OwnedListingStatus)
}
