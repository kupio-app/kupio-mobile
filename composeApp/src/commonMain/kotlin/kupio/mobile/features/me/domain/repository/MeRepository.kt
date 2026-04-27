package kupio.mobile.features.me.domain.repository

import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.UserListingStats

interface MeRepository {
    suspend fun getStats(): UserListingStats
    suspend fun getMyListings(): List<OwnedListing>
}
