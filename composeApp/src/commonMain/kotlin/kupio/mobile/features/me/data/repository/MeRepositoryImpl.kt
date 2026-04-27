package kupio.mobile.features.me.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.me.data.remote.MeApi
import kupio.mobile.features.me.domain.model.OwnedListing
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
}
