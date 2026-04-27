package kupio.mobile.features.listings.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.listings.data.remote.ListingsApi
import kupio.mobile.features.listings.data.remote.ListingStatusUpdateRequestDto
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.data.remote.toDto
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
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

    override suspend fun createListing(listing: CreateListing): Listing =
        authenticatedApiClient.request { authorize ->
            listingsApi.createListing(authorize, listing.toDto())
        }.toDomain()

    override suspend fun updateListingStatus(
        listingId: String,
        status: ListingStatus,
    ): Listing = authenticatedApiClient.request { authorize ->
        listingsApi.updateListingStatus(
            authorize = authorize,
            listingId = listingId,
            request = ListingStatusUpdateRequestDto(status.toDto()),
        )
    }.toDomain()

    override suspend fun uploadListingImages(
        listingId: String,
        images: List<ListingImageUpload>,
    ) {
        if (images.isEmpty()) return
        authenticatedApiClient.request { authorize ->
            listingsApi.uploadListingImages(authorize, listingId, images)
        }
    }
}
