package kupio.mobile.features.saved.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.offline.OfflineMutationStore
import kupio.mobile.core.offline.OfflineSyncManager
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.saved.data.remote.FavouritesApi
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FavouritesRepositoryImpl(
    private val favouritesApi: FavouritesApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val offlineStore: OfflineMutationStore,
    private val offlineSyncManager: OfflineSyncManager,
    private val appScope: CoroutineScope,
) : FavouritesRepository {

    override suspend fun getFavourites(limit: Int, cursor: String?): ListingFeed =
        runCatching {
            authenticatedApiClient.request { authorize ->
                favouritesApi.getFavourites(authorize, limit, cursor)
            }.toDomain().also { offlineStore.cacheFeed(it) }
        }.getOrElse {
            ListingFeed(emptyList(), nextCursor = null)
        }

    override suspend fun getFavouriteIds(): Set<String> =
        runCatching {
            authenticatedApiClient.request { authorize ->
                favouritesApi.getFavouriteIds(authorize)
            }.listingsIds.toSet().also { offlineStore.replaceFavouriteIds(it) }
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
        appScope.launch {
            runCatching { offlineSyncManager.syncPending() }
        }
    }
}
