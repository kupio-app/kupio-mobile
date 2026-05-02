package kupio.mobile.features.listings.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.listings.data.remote.ListingsApi
import kupio.mobile.features.listings.data.remote.ListingStatusUpdateRequestDto
import kupio.mobile.features.listings.data.remote.UpdateListingImagesOrderRequestDto
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.data.remote.toDto
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImage
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.toApiParams
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ListingsRepositoryImpl(
    private val listingsApi: ListingsApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : ListingsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun searchListings(
        filters: SearchFilters,
        cursor: String?,
        limit: Int,
    ): ListingFeed {
        val (isFree, isTradable, customFilters) = filters.toApiParams()
        val filtersJson = if (customFilters.isNotEmpty()) {
            json.encodeToString(customFilters)
        } else {
            null
        }
        return listingsApi.getListings(
            query = filters.query.takeIf { it.isNotBlank() },
            categoryId = filters.categoryId,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            isFree = isFree,
            isTradable = isTradable,
            filters = filtersJson,
            limit = limit,
            cursor = cursor,
        ).toDomain()
    }

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

    override suspend fun getListingDetail(id: String): Listing =
        authenticatedApiClient.request { authorize ->
            listingsApi.getListing(authorize, id, countSeen = true)
        }.toDomain()

    override suspend fun createListing(listing: CreateListing): Listing =
        authenticatedApiClient.request { authorize ->
            listingsApi.createListing(authorize, listing.toDto())
        }.toDomain()

    override suspend fun updateListing(
        listingId: String,
        listing: CreateListing,
        phone: String?,
        contactName: String?,
        isCallsDisabled: Boolean,
    ): Listing = authenticatedApiClient.request { authorize ->
        listingsApi.updateListing(
            authorize = authorize,
            listingId = listingId,
            request = listing.toDto(
                phone = phone,
                contactName = contactName,
                isCallsDisabled = isCallsDisabled,
            ),
        )
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
    ): List<ListingImage> {
        if (images.isEmpty()) return emptyList()
        return authenticatedApiClient.request { authorize ->
            listingsApi.uploadListingImages(authorize, listingId, images)
        }.map { it.toDomain() }
    }

    override suspend fun deleteListingImage(
        listingId: String,
        imageId: String,
    ) {
        authenticatedApiClient.request { authorize ->
            listingsApi.deleteListingImage(authorize, listingId, imageId)
        }
    }

    override suspend fun updateListingImagesOrder(
        listingId: String,
        imageIds: List<String>,
    ) {
        authenticatedApiClient.request { authorize ->
            listingsApi.updateListingImagesOrder(
                authorize = authorize,
                listingId = listingId,
                request = UpdateListingImagesOrderRequestDto(imageIds),
            )
        }
    }
}
