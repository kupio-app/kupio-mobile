package kupio.mobile.features.saved.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.saved.data.remote.FavouritesApi
import kupio.mobile.features.saved.domain.repository.FavouritesRepository

class FavouritesRepositoryImpl(
    private val favouritesApi: FavouritesApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : FavouritesRepository {

    override suspend fun getFavourites(limit: Int, cursor: String?): ListingFeed =
        authenticatedApiClient.request { authorize ->
            favouritesApi.getFavourites(authorize, limit, cursor)
        }.toDomain()

    override suspend fun getFavouriteIds(): Set<String> =
        authenticatedApiClient.request { authorize ->
            favouritesApi.getFavouriteIds(authorize)
        }.listingsIds.toSet()

    override suspend fun addFavourite(listingId: String) {
        authenticatedApiClient.request { authorize ->
            favouritesApi.addFavourite(authorize, listingId)
        }
    }

    override suspend fun removeFavourite(listingId: String) {
        authenticatedApiClient.request { authorize ->
            favouritesApi.removeFavourite(authorize, listingId)
        }
    }
}
