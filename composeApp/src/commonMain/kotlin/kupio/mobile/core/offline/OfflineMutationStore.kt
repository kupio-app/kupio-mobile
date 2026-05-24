package kupio.mobile.core.offline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kupio.mobile.core.offline.db.CachedFavouriteEntity
import kupio.mobile.core.offline.db.KupioDatabase
import kupio.mobile.core.offline.db.LocalListingEntity
import kupio.mobile.core.offline.db.PendingListingImageEntity
import kupio.mobile.core.offline.db.PendingSyncOperationEntity
import kupio.mobile.core.offline.files.OfflineFileStore
import kupio.mobile.features.listings.data.remote.ListingRequestDto
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.data.remote.toDto
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kotlin.time.Clock

class OfflineMutationStore(
    private val database: KupioDatabase,
    private val fileStore: OfflineFileStore,
) {
    private val listingsDao = database.listingsDao()
    private val favouritesDao = database.favouritesDao()
    private val pendingSyncDao = database.pendingSyncDao()
    private val pendingImagesDao = database.pendingImagesDao()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun cacheFeed(feed: ListingFeed) {
        listingsDao.upsertAll(
            feed.listings.mapNotNull { listing ->
                listing.toRemoteCacheEntity()
            },
        )
    }

    suspend fun cacheListing(listing: Listing) {
        listing.toRemoteCacheEntity()?.let { listingsDao.upsert(it) }
    }

    suspend fun getListing(id: String): Listing? =
        listingsDao.getByIdOrServerId(id)?.toDomain()

    suspend fun getOwnedListings(userId: String): List<OwnedListing> =
        listingsDao.getOwned(userId).map { it.toOwnedListing() }

    suspend fun getFavouriteListings(): ListingFeed {
        val desiredIds = favouritesDao.getDesired().map { it.listingId }
        if (desiredIds.isEmpty()) return ListingFeed(emptyList(), nextCursor = null)

        val listingsById = listingsDao.getByIdsOrServerIds(desiredIds)
            .flatMap { entity ->
                listOfNotNull(entity.id to entity, entity.serverId?.let { it to entity })
            }
            .toMap()

        return ListingFeed(
            listings = desiredIds.mapNotNull { listingsById[it]?.toDomain() },
            nextCursor = null,
        )
    }

    suspend fun applyFavouriteOverrides(feed: ListingFeed): ListingFeed {
        val favouriteOverrides = favouritesDao.getAll()
        if (favouriteOverrides.isEmpty()) return feed

        val overridesById = favouriteOverrides.associateBy { it.listingId }
        val removedIds = favouriteOverrides.filterNot { it.desired }.map { it.listingId }.toSet()
        val localDesiredListings = getFavouriteListings().listings
        val remoteListings = feed.listings.filterNot { it.id in removedIds || overridesById[it.id]?.desired == false }
        val remoteIds = remoteListings.map { it.id }.toSet()

        return feed.copy(
            listings = remoteListings + localDesiredListings.filterNot { it.id in remoteIds },
        )
    }

    suspend fun cacheOwnedListings(
        listings: List<OwnedListing>,
        userId: String,
    ) {
        val now = nowMs()
        listingsDao.upsertAll(
            listings.mapNotNull { listing ->
                val existing = listingsDao.getByIdOrServerId(listing.id)
                if (existing?.isLocallyModified() == true) {
                    null
                } else {
                    listing.toEntity(userId, now, existing)
                }
            },
        )
    }

    suspend fun getCachedFavourites(): Set<String> =
        favouritesDao.getDesired().map { it.listingId }.toSet()

    fun observeFavouriteIds(): Flow<Set<String>> =
        favouritesDao.observeDesiredIds().map { it.toSet() }

    suspend fun replaceFavouriteIds(ids: Set<String>) {
        val now = nowMs()
        val pendingOverrides = favouritesDao.getUnsynced()
        favouritesDao.clearAndReplaceAll(
            ids.map { id ->
                CachedFavouriteEntity(
                    listingId = id,
                    desired = true,
                    synced = true,
                    updatedAtMs = now,
                )
            },
        )
        pendingOverrides.forEach { favouritesDao.upsert(it.copy(updatedAtMs = now)) }
    }

    suspend fun createLocalListing(
        listing: CreateListing,
        userId: String,
    ): Listing {
        val now = nowMs()
        val localId = "local-$now"
        val request = listing.toRequestDto()
        val localListing = request.toLocalListing(
            id = localId,
            userId = userId,
            status = ListingStatus.INACTIVE,
            syncState = OfflineSyncState.PENDING,
            createdAt = now.toString(),
        )
        listingsDao.upsert(localListing.toEntity(OfflineSyncState.PENDING))
        pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = PendingOperationType.CREATE_LISTING.name,
                listingId = localId,
                payloadJson = json.encodeToString(CreateListingOperationPayload(request)),
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        return localListing
    }

    suspend fun queueListingUpdate(
        listingId: String,
        request: ListingRequestDto,
        currentListing: Listing?,
    ): Listing {
        val now = nowMs()
        val updated = request.toLocalListing(
            id = currentListing?.id ?: listingId,
            userId = currentListing?.userId.orEmpty(),
            status = currentListing?.status ?: ListingStatus.INACTIVE,
            syncState = OfflineSyncState.PENDING,
            createdAt = currentListing?.createdAt ?: now.toString(),
            seenCount = currentListing?.seenCount ?: 0,
            primaryImageUrl = currentListing?.primaryImageUrl,
            images = currentListing?.images.orEmpty().map {
                CachedListingImage(it.id, it.url, it.sortOrder)
            },
            updatedAt = currentListing?.updatedAt,
        )
        listingsDao.upsert(updated.toEntity(OfflineSyncState.PENDING, now))
        val operation = PendingSyncOperationEntity(
            type = PendingOperationType.UPDATE_LISTING.name,
            listingId = listingId,
            payloadJson = json.encodeToString(UpdateListingOperationPayload(request)),
            state = OfflineSyncState.PENDING.name,
            attempts = 0,
            lastError = null,
            createdAtMs = now,
            updatedAtMs = now,
        )
        val operationId = pendingSyncDao.insert(operation)
        pendingSyncDao.deleteOtherRunnableForListing(
            listingId = listingId,
            type = PendingOperationType.UPDATE_LISTING.name,
            keepId = operationId,
        )
        return updated
    }

    suspend fun queueListingStatus(
        listingId: String,
        status: ListingStatus,
        currentListing: Listing?,
    ): Listing? {
        val now = nowMs()
        val updated = currentListing?.copy(
            status = status,
            syncState = OfflineSyncState.PENDING,
        )
        if (updated != null) {
            listingsDao.upsert(updated.toEntity(OfflineSyncState.PENDING, now))
        }
        val operationId = pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = PendingOperationType.UPDATE_LISTING_STATUS.name,
                listingId = listingId,
                payloadJson = json.encodeToString(
                    UpdateListingStatusOperationPayload(status.toDto()),
                ),
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        pendingSyncDao.deleteOtherRunnableForListing(
            listingId = listingId,
            type = PendingOperationType.UPDATE_LISTING_STATUS.name,
            keepId = operationId,
        )
        return updated
    }

    suspend fun queueImageUpload(
        listingId: String,
        images: List<ListingImageUpload>,
    ) {
        if (images.isEmpty()) return
        val now = nowMs()
        val operationId = pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = PendingOperationType.UPLOAD_LISTING_IMAGES.name,
                listingId = listingId,
                payloadJson = "{}",
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        pendingImagesDao.insertAll(
            images.mapIndexed { index, image ->
                PendingListingImageEntity(
                    operationId = operationId,
                    fileName = image.fileName,
                    mimeType = image.mimeType,
                    filePath = fileStore.saveListingImage(image.fileName, image.bytes),
                    sortOrder = index,
                )
            },
        )
    }

    suspend fun queueDeleteImage(listingId: String, imageId: String) {
        val now = nowMs()
        pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = PendingOperationType.DELETE_LISTING_IMAGE.name,
                listingId = listingId,
                payloadJson = json.encodeToString(DeleteListingImageOperationPayload(imageId)),
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
    }

    suspend fun queueImagesOrder(listingId: String, imageIds: List<String>) {
        val now = nowMs()
        val operationId = pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = PendingOperationType.UPDATE_LISTING_IMAGES_ORDER.name,
                listingId = listingId,
                payloadJson = json.encodeToString(UpdateListingImagesOrderOperationPayload(imageIds)),
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        pendingSyncDao.deleteOtherRunnableForListing(
            listingId = listingId,
            type = PendingOperationType.UPDATE_LISTING_IMAGES_ORDER.name,
            keepId = operationId,
        )
    }

    suspend fun setFavouriteDesired(listingId: String, desired: Boolean) {
        val now = nowMs()
        favouritesDao.upsert(
            CachedFavouriteEntity(
                listingId = listingId,
                desired = desired,
                synced = false,
                updatedAtMs = now,
            ),
        )
        val type = if (desired) PendingOperationType.ADD_FAVOURITE else PendingOperationType.REMOVE_FAVOURITE
        val operationId = pendingSyncDao.insert(
            PendingSyncOperationEntity(
                type = type.name,
                listingId = listingId,
                payloadJson = json.encodeToString(FavouriteOperationPayload(listingId)),
                state = OfflineSyncState.PENDING.name,
                attempts = 0,
                lastError = null,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
        pendingSyncDao.deleteOtherRunnableForListing(
            listingId = listingId,
            type = PendingOperationType.ADD_FAVOURITE.name,
            keepId = operationId,
        )
        pendingSyncDao.deleteOtherRunnableForListing(
            listingId = listingId,
            type = PendingOperationType.REMOVE_FAVOURITE.name,
            keepId = operationId,
        )
    }

    suspend fun runnableOperations(limit: Int): List<PendingSyncOperationEntity> =
        pendingSyncDao.getRunnable(limit)

    suspend fun markSyncing(operation: PendingSyncOperationEntity) {
        pendingSyncDao.updateState(
            id = operation.id,
            state = OfflineSyncState.SYNCING.name,
            attempts = operation.attempts,
            lastError = null,
            updatedAtMs = nowMs(),
        )
    }

    suspend fun markFailed(operation: PendingSyncOperationEntity, throwable: Throwable) {
        val now = nowMs()
        pendingSyncDao.updateState(
            id = operation.id,
            state = OfflineSyncState.FAILED.name,
            attempts = operation.attempts + 1,
            lastError = throwable.message,
            updatedAtMs = now,
        )
        operation.listingId?.let { listingId ->
            getListing(listingId)?.let {
                listingsDao.upsert(
                    it.copy(
                        syncState = OfflineSyncState.FAILED,
                        syncError = throwable.message,
                    ).toEntity(OfflineSyncState.FAILED, now, throwable.message),
                )
            }
        }
    }

    suspend fun markSynced(operation: PendingSyncOperationEntity) {
        pendingSyncDao.deleteById(operation.id)
        if (operation.type == PendingOperationType.UPLOAD_LISTING_IMAGES.name) {
            val images = pendingImagesDao.getForOperation(operation.id)
            images.forEach { fileStore.delete(it.filePath) }
            pendingImagesDao.deleteForOperation(operation.id)
        }
    }

    suspend fun resetStuckSyncing() {
        pendingSyncDao.resetStuckSyncing(nowMs())
    }

    suspend fun resolveListingId(listingId: String): String? {
        val listing = listingsDao.getByIdOrServerId(listingId) ?: return listingId
        return listing.serverId ?: listing.id.takeUnless { it.startsWith("local-") }
    }

    suspend fun replaceLocalListingId(localId: String, remote: Listing) {
        val now = nowMs()
        listingsDao.upsert(
            remote.copy(id = localId).toEntity(
                syncState = OfflineSyncState.SYNCED,
                modifiedAtMs = now,
                serverIdOverride = remote.id,
            ),
        )
    }

    suspend fun pendingImages(operationId: Long): List<PendingListingImageEntity> =
        pendingImagesDao.getForOperation(operationId)

    suspend fun readImage(path: String): ByteArray = fileStore.read(path)

    suspend fun setFavouriteSynced(listingId: String) {
        favouritesDao.setSynced(listingId, synced = true, updatedAtMs = nowMs())
    }

    suspend fun markListingSynced(listingId: String, remote: Listing? = null) {
        val now = nowMs()
        val listing = remote ?: getListing(listingId) ?: return
        listingsDao.upsert(listing.copy(syncState = OfflineSyncState.SYNCED).toEntity(OfflineSyncState.SYNCED, now))
    }

    fun decodeCreatePayload(operation: PendingSyncOperationEntity): CreateListingOperationPayload =
        json.decodeFromString(operation.payloadJson)

    fun decodeUpdatePayload(operation: PendingSyncOperationEntity): UpdateListingOperationPayload =
        json.decodeFromString(operation.payloadJson)

    fun decodeStatusPayload(operation: PendingSyncOperationEntity): UpdateListingStatusOperationPayload =
        json.decodeFromString(operation.payloadJson)

    fun decodeDeleteImagePayload(operation: PendingSyncOperationEntity): DeleteListingImageOperationPayload =
        json.decodeFromString(operation.payloadJson)

    fun decodeImagesOrderPayload(operation: PendingSyncOperationEntity): UpdateListingImagesOrderOperationPayload =
        json.decodeFromString(operation.payloadJson)

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private suspend fun Listing.toRemoteCacheEntity(): LocalListingEntity? {
        val existing = listingsDao.getByIdOrServerId(id)
        if (existing?.isLocallyModified() == true) return null
        return copy(id = existing?.id ?: id).toEntity(
            syncState = OfflineSyncState.SYNCED,
            serverIdOverride = id,
        )
    }

    private fun LocalListingEntity.isLocallyModified(): Boolean =
        syncState != OfflineSyncState.SYNCED.name

    private fun CreateListing.toRequestDto(): ListingRequestDto = ListingRequestDto(
        title = title,
        description = description,
        price = price,
        isFree = isFree,
        isTradable = isTradable,
        currency = currency.toDto(),
        categoryId = categoryId,
        customFilters = customFilters.toJsonObject().takeUnless { it.isEmpty() },
    )

    private fun Map<String, CustomFilterPayloadValue>.toJsonObject(): JsonObject =
        kotlinx.serialization.json.buildJsonObject {
            this@toJsonObject.forEach { (key, value) ->
                put(
                    key,
                    when (value) {
                        is CustomFilterPayloadValue.Text -> JsonPrimitive(value.value)
                        is CustomFilterPayloadValue.Number -> JsonPrimitive(value.value)
                        is CustomFilterPayloadValue.BooleanValue -> JsonPrimitive(value.value)
                    },
                )
            }
        }

    private fun ListingRequestDto.toLocalListing(
        id: String,
        userId: String,
        status: ListingStatus,
        syncState: OfflineSyncState,
        createdAt: String,
        seenCount: Int = 0,
        primaryImageUrl: String? = null,
        images: List<CachedListingImage> = emptyList(),
        updatedAt: String? = null,
    ): Listing = Listing(
        id = id,
        title = title,
        description = description,
        price = price,
        currency = currency.toDomain(),
        status = status,
        primaryImageUrl = primaryImageUrl,
        images = images.map {
            kupio.mobile.features.listings.domain.model.ListingImage(it.id, it.url, it.sortOrder)
        },
        imageUrls = images.sortedBy { it.sortOrder }.map { it.url },
        createdAt = createdAt,
        updatedAt = updatedAt,
        userId = userId,
        categoryId = categoryId,
        categoryName = "",
        seenCount = seenCount,
        phone = phone,
        contactName = contactName,
        isCallsDisabled = isCallsDisabled,
        isFree = isFree,
        isTradable = isTradable,
        customFilters = customFilters.toDisplayMap(),
        syncState = syncState,
    )

    private fun JsonObject?.toDisplayMap(): Map<String, String> {
        if (this == null) return emptyMap()
        return entries.mapNotNull { (key, value) ->
            val text = when (value) {
                is JsonPrimitive -> value.contentOrNull
                else -> value.toString()
            }?.takeIf { it.isNotBlank() }
            text?.let { key to it }
        }.toMap()
    }

    private fun Listing.toEntity(
        syncState: OfflineSyncState,
        modifiedAtMs: Long = nowMs(),
        error: String? = syncError,
        serverIdOverride: String? = null,
    ): LocalListingEntity = LocalListingEntity(
        id = id,
        serverId = serverIdOverride ?: id.takeUnless { it.startsWith("local-") },
        title = title,
        description = description,
        price = price,
        currency = currency.name,
        status = status.name,
        primaryImageUrl = primaryImageUrl,
        imagesJson = json.encodeToString(images.map { CachedListingImage(it.id, it.url, it.sortOrder) }),
        createdAt = createdAt,
        updatedAt = updatedAt,
        userId = userId,
        categoryId = categoryId,
        categoryName = categoryName,
        seenCount = seenCount,
        phone = phone,
        contactName = contactName,
        isCallsDisabled = isCallsDisabled,
        isFree = isFree,
        isTradable = isTradable,
        customFiltersJson = json.encodeToString(customFilters),
        syncState = syncState.name,
        lastError = error,
        lastModifiedAtMs = modifiedAtMs,
    )

    private fun LocalListingEntity.toDomain(): Listing {
        val images = json.decodeFromString<List<CachedListingImage>>(imagesJson).sortedBy { it.sortOrder }
        return Listing(
            id = id,
            title = title,
            description = description,
            price = price,
            currency = Currency.valueOf(currency),
            status = ListingStatus.valueOf(status),
            primaryImageUrl = primaryImageUrl,
            images = images.map {
                kupio.mobile.features.listings.domain.model.ListingImage(it.id, it.url, it.sortOrder)
            },
            imageUrls = images.map { it.url },
            createdAt = createdAt,
            updatedAt = updatedAt,
            userId = userId,
            categoryId = categoryId,
            categoryName = categoryName,
            seenCount = seenCount,
            phone = phone,
            contactName = contactName,
            isCallsDisabled = isCallsDisabled,
            isFree = isFree,
            isTradable = isTradable,
            customFilters = json.decodeFromString(customFiltersJson),
            syncState = OfflineSyncState.valueOf(syncState),
            syncError = lastError,
        )
    }

    private fun LocalListingEntity.toOwnedListing(): OwnedListing = OwnedListing(
        id = id,
        title = title,
        price = price,
        currency = Currency.valueOf(currency),
        primaryImageUrl = primaryImageUrl,
        status = when (ListingStatus.valueOf(status)) {
            ListingStatus.ACTIVE -> OwnedListingStatus.ACTIVE
            ListingStatus.INACTIVE -> OwnedListingStatus.INACTIVE
            ListingStatus.DRAFT -> OwnedListingStatus.DRAFT
            ListingStatus.PLANNED -> OwnedListingStatus.PLANNED
            ListingStatus.SOLD -> OwnedListingStatus.SOLD
        },
        seenCount = seenCount,
        favouritesCount = 0,
        chatsCount = 0,
        isPromoted = false,
        promotionExpiresAt = null,
        syncState = OfflineSyncState.valueOf(syncState),
        syncError = lastError,
    )

    private fun OwnedListing.toEntity(
        userId: String,
        modifiedAtMs: Long,
        existing: LocalListingEntity?,
    ): LocalListingEntity = LocalListingEntity(
        id = existing?.id ?: id,
        serverId = id,
        title = title,
        description = existing?.description.orEmpty(),
        price = price,
        currency = currency.name,
        status = when (status) {
            OwnedListingStatus.ACTIVE -> ListingStatus.ACTIVE
            OwnedListingStatus.INACTIVE -> ListingStatus.INACTIVE
            OwnedListingStatus.DRAFT -> ListingStatus.DRAFT
            OwnedListingStatus.PLANNED -> ListingStatus.PLANNED
            OwnedListingStatus.SOLD -> ListingStatus.SOLD
        }.name,
        primaryImageUrl = primaryImageUrl,
        imagesJson = existing?.imagesJson ?: primaryImageUrl
            ?.let { json.encodeToString(listOf(CachedListingImage(id = "primary-$id", url = it, sortOrder = 0))) }
            ?: json.encodeToString(emptyList<CachedListingImage>()),
        createdAt = existing?.createdAt ?: modifiedAtMs.toString(),
        updatedAt = existing?.updatedAt,
        userId = userId,
        categoryId = existing?.categoryId ?: 0,
        categoryName = existing?.categoryName.orEmpty(),
        seenCount = seenCount,
        phone = existing?.phone,
        contactName = existing?.contactName,
        isCallsDisabled = existing?.isCallsDisabled ?: false,
        isFree = existing?.isFree ?: false,
        isTradable = existing?.isTradable ?: false,
        customFiltersJson = existing?.customFiltersJson ?: json.encodeToString(emptyMap<String, String>()),
        syncState = syncState.name,
        lastError = syncError,
        lastModifiedAtMs = modifiedAtMs,
    )
}
