package kupio.mobile.features.listings.domain.repository

import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed

interface ListingsRepository {
    suspend fun getFeed(
        limit: Int = 20,
        cursor: String? = null,
        query: String? = null,
        categoryId: Int? = null,
    ): ListingFeed

    suspend fun getListing(id: String): Listing
}
