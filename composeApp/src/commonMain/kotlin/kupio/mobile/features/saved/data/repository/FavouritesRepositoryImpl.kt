package kupio.mobile.features.saved.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.offline.OfflineMutationStore
import kupio.mobile.core.offline.OfflineSyncScheduler
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.saved.data.remote.FavouritesApi
import kupio.mobile.features.saved.domain.repository.FavouritesRepository

class FavouritesRepositoryImpl(
    private val favouritesApi: FavouritesApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val offlineStore: OfflineMutationStore,
    private val offlineSyncScheduler: OfflineSyncScheduler,
) : FavouritesRepository {

    override suspend fun getFavourites(limit: Int, cursor: String?): ListingFeed =
        runCatching {
            authenticatedApiClient.request { authorize ->
                favouritesApi.getFavourites(authorize, limit, cursor)
            }.toDomain()
                .also { offlineStore.cacheFeed(it) }
                .let { offlineStore.applyFavouriteOverrides(it) }
        }.getOrElse {
            offlineStore.getFavouriteListings()
        }

    override suspend fun getFavouriteIds(): Set<String> =
        runCatching {
            authenticatedApiClient.request { authorize ->
                favouritesApi.getFavouriteIds(authorize)
            }.listingsIds.toSet()
                .also { offlineStore.replaceFavouriteIds(it) }
                .let { offlineStore.getCachedFavourites() }
        }.getOrElse {
            offlineStore.getCachedFavourites()
        }

    override suspend fun addFavourite(listingId: String) {
        offlineStore.setFavouriteDesired(listingId, desired = true)
        launchSync()
    }

    override suspend fun removeFavourite(listingId: String) {
        offlineStore.setFavouriteDesired(listingId, desired = false)
        launchSync()
    }

    private fun launchSync() {
        offlineSyncScheduler.requestSync()
    }
}
