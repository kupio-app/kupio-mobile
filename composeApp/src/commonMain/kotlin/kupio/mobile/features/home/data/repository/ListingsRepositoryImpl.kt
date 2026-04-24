package kupio.mobile.features.home.data.repository

import kupio.mobile.features.home.data.remote.ListingsApi
import kupio.mobile.features.home.data.remote.toDomain
import kupio.mobile.features.home.domain.model.ListingFeed
import kupio.mobile.features.home.domain.repository.ListingsRepository

class ListingsRepositoryImpl(
    private val listingsApi: ListingsApi,
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
}
