package kupio.mobile.features.listings.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.offline.OfflineMutationStore
import kupio.mobile.core.offline.OfflineSyncManager
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.listings.data.remote.ListingsApi
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ListingsRepositoryImpl(
    private val listingsApi: ListingsApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val offlineStore: OfflineMutationStore,
    private val offlineSyncManager: OfflineSyncManager,
    private val sessionManager: AuthSessionManager,
    private val appScope: CoroutineScope,
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
        return runCatching {
            listingsApi.getListings(
                query = filters.query.takeIf { it.isNotBlank() },
                categoryId = filters.categoryId,
                minPrice = filters.minPrice,
                maxPrice = filters.maxPrice,
                isFree = isFree,
                isTradable = isTradable,
                filters = filtersJson,
                limit = limit,
                cursor = cursor,
            ).toDomain().also { offlineStore.cacheFeed(it) }
        }.getOrElse {
            ListingFeed(emptyList(), nextCursor = null)
        }
    }

    override suspend fun getFeed(
        limit: Int,
        cursor: String?,
        query: String?,
        categoryId: Int?,
    ): ListingFeed = runCatching {
        listingsApi.getListings(
            query = query,
            categoryId = categoryId,
            limit = limit,
            cursor = cursor,
        ).toDomain().also { offlineStore.cacheFeed(it) }
    }.getOrElse {
        ListingFeed(emptyList(), nextCursor = null)
    }

    override suspend fun getListing(id: String): Listing =
        runCatching {
            authenticatedApiClient.request { authorize ->
                listingsApi.getListing(authorize, id)
            }.toDomain().also { offlineStore.cacheListing(it) }
        }.getOrElse { throwable ->
            offlineStore.getListing(id) ?: throw throwable
        }

    override suspend fun getListingDetail(id: String): Listing =
        runCatching {
            authenticatedApiClient.request { authorize ->
                listingsApi.getListing(authorize, id, countSeen = true)
            }.toDomain().also { offlineStore.cacheListing(it) }
        }.getOrElse { throwable ->
            offlineStore.getListing(id) ?: throw throwable
        }

    override suspend fun createListing(listing: CreateListing): Listing {
        val local = offlineStore.createLocalListing(
            listing = listing,
            userId = sessionManager.currentUserId(),
        )
        launchSync()
        return local
    }

    override suspend fun updateListing(
        listingId: String,
        listing: CreateListing,
        phone: String?,
        contactName: String?,
        isCallsDisabled: Boolean,
    ): Listing {
        val currentListing = offlineStore.getListing(listingId)
            ?: runCatching { getListing(listingId) }.getOrNull()
        val updated = offlineStore.queueListingUpdate(
            listingId = listingId,
            request = listing.toDto(
                phone = phone,
                contactName = contactName,
                isCallsDisabled = isCallsDisabled,
            ),
            currentListing = currentListing,
        )
        launchSync()
        return updated
    }

    override suspend fun updateListingStatus(
        listingId: String,
        status: ListingStatus,
    ): Listing {
        val currentListing = offlineStore.getListing(listingId)
            ?: runCatching { getListing(listingId) }.getOrNull()
        val updated = offlineStore.queueListingStatus(listingId, status, currentListing)
        launchSync()
        return updated ?: currentListing ?: error("Listing not found")
    }

    override suspend fun uploadListingImages(
        listingId: String,
        images: List<ListingImageUpload>,
    ): List<ListingImage> {
        if (images.isEmpty()) return emptyList()
        offlineStore.queueImageUpload(listingId, images)
        launchSync()
        return emptyList()
    }

    override suspend fun deleteListingImage(
        listingId: String,
        imageId: String,
    ) {
        offlineStore.queueDeleteImage(listingId, imageId)
        launchSync()
    }

    override suspend fun updateListingImagesOrder(
        listingId: String,
        imageIds: List<String>,
    ) {
        offlineStore.queueImagesOrder(listingId, imageIds)
        launchSync()
    }

    private fun launchSync() {
        appScope.launch {
            runCatching { offlineSyncManager.syncPending() }
        }
    }
}
