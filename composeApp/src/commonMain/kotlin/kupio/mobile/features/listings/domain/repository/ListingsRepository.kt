package kupio.mobile.features.listings.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImage
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.search.domain.model.SearchFilters

interface ListingsRepository {
    val listingUpdates: SharedFlow<Listing>
    suspend fun searchListings(
        filters: SearchFilters,
        cursor: String? = null,
        limit: Int = 20,
    ): ListingFeed

    suspend fun getFeed(
        limit: Int = 20,
        cursor: String? = null,
        query: String? = null,
        categoryId: Int? = null,
    ): ListingFeed

    suspend fun getListing(id: String): Listing

    suspend fun getListingDetail(id: String): Listing

    suspend fun createListing(listing: CreateListing): Listing

    suspend fun updateListing(
        listingId: String,
        listing: CreateListing,
        phone: String? = null,
        contactName: String? = null,
        isCallsDisabled: Boolean = false,
    ): Listing

    suspend fun updateListingStatus(
        listingId: String,
        status: ListingStatus,
    ): Listing

    suspend fun uploadListingImages(
        listingId: String,
        images: List<ListingImageUpload>,
    ): List<ListingImage>

    suspend fun deleteListingImage(
        listingId: String,
        imageId: String,
    )

    suspend fun updateListingImagesOrder(
        listingId: String,
        imageIds: List<String>,
    )
}
