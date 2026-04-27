package kupio.mobile.features.listings.domain.repository

import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus

interface ListingsRepository {
    suspend fun getFeed(
        limit: Int = 20,
        cursor: String? = null,
        query: String? = null,
        categoryId: Int? = null,
    ): ListingFeed

    suspend fun getListing(id: String): Listing

    suspend fun createListing(listing: CreateListing): Listing

    suspend fun updateListingStatus(
        listingId: String,
        status: ListingStatus,
    ): Listing

    suspend fun uploadListingImages(
        listingId: String,
        images: List<ListingImageUpload>,
    )
}
