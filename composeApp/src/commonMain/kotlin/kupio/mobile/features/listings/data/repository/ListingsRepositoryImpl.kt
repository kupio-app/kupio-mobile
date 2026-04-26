package kupio.mobile.features.listings.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.listings.data.remote.ListingsApi
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ListingsRepositoryImpl(
    private val listingsApi: ListingsApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : ListingsRepository {

    override suspend fun getFeed(
        limit: Int,
        cursor: String?,
        query: String?,
        categoryId: Int?,
    ): ListingFeed = listingsApi.getListings(
        query = query,
        categoryId = categoryId,
        limit = limit,
        cursor = cursor,
    ).toDomain()

    override suspend fun getListing(id: String): Listing =
        authenticatedApiClient.request { authorize ->
            listingsApi.getListing(authorize, id)
        }.toDomain()
}
