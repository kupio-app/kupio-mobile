package kupio.mobile.features.home.domain.repository

import kupio.mobile.features.home.domain.model.ListingFeed

interface ListingsRepository {
    suspend fun getFeed(
        limit: Int = 20,
        cursor: String? = null,
        query: String? = null,
        categoryId: Int? = null,
    ): ListingFeed
}
