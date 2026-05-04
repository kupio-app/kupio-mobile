package kupio.mobile.core.offline

import kupio.mobile.core.network.ApiException
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.listings.data.remote.ListingStatusUpdateRequestDto
import kupio.mobile.features.listings.data.remote.ListingsApi
import kupio.mobile.features.listings.data.remote.UpdateListingImagesOrderRequestDto
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.saved.data.remote.FavouritesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineSyncManager(
    private val store: OfflineMutationStore,
    private val listingsApi: ListingsApi,
    private val favouritesApi: FavouritesApi,
    private val apiClient: AuthenticatedApiClient,
) {
    private val syncMutex = Mutex()

    suspend fun syncPending(limit: Int = DefaultBatchSize) {
        syncMutex.withLock {
            var firstFailure: Throwable? = null
            store.resetStuckSyncing()
            store.runnableOperations(limit).forEach { operation ->
                store.markSyncing(operation)
                runCatching {
                    syncOperation(operation)
                }.onSuccess {
                    store.markSynced(operation)
                }.onFailure { throwable ->
                    if (throwable is AuthSessionExpiredException) throw throwable
                    store.markFailed(operation, throwable)
                    if (firstFailure == null) firstFailure = throwable
                }
            }
            firstFailure?.let { throw OfflineSyncFailedException(it) }
        }
    }

    private suspend fun syncOperation(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        when (PendingOperationType.valueOf(operation.type)) {
            PendingOperationType.CREATE_LISTING -> syncCreateListing(operation)
            PendingOperationType.UPDATE_LISTING -> syncUpdateListing(operation)
            PendingOperationType.UPDATE_LISTING_STATUS -> syncUpdateListingStatus(operation)
            PendingOperationType.UPLOAD_LISTING_IMAGES -> syncUploadImages(operation)
            PendingOperationType.DELETE_LISTING_IMAGE -> syncDeleteImage(operation)
            PendingOperationType.UPDATE_LISTING_IMAGES_ORDER -> syncUpdateImagesOrder(operation)
            PendingOperationType.ADD_FAVOURITE -> syncAddFavourite(operation)
            PendingOperationType.REMOVE_FAVOURITE -> syncRemoveFavourite(operation)
        }
    }

    private suspend fun syncCreateListing(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val payload = store.decodeCreatePayload(operation)
        val localId = requireNotNull(operation.listingId)
        val remote = apiClient.request { authorize ->
            listingsApi.createListing(authorize, payload.request)
        }.toDomain()
        store.replaceLocalListingId(localId, remote)
    }

    private suspend fun syncUpdateListing(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val payload = store.decodeUpdatePayload(operation)
        val remote = apiClient.request { authorize ->
            listingsApi.updateListing(authorize, listingId, payload.request)
        }.toDomain()
        store.markListingSynced(listingId, remote)
    }

    private suspend fun syncUpdateListingStatus(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val payload = store.decodeStatusPayload(operation)
        val remote = apiClient.request { authorize ->
            listingsApi.updateListingStatus(
                authorize = authorize,
                listingId = listingId,
                request = ListingStatusUpdateRequestDto(payload.status),
            )
        }.toDomain()
        store.markListingSynced(listingId, remote)
    }

    private suspend fun syncUploadImages(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val images = store.pendingImages(operation.id).map { image ->
            ListingImageUpload(
                fileName = image.fileName,
                mimeType = image.mimeType,
                bytes = store.readImage(image.filePath),
            )
        }
        if (images.isEmpty()) return
        apiClient.request { authorize ->
            listingsApi.uploadListingImages(authorize, listingId, images)
        }
        store.markListingSynced(listingId)
    }

    private suspend fun syncDeleteImage(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val payload = store.decodeDeleteImagePayload(operation)
        val result = runCatching {
            apiClient.request { authorize ->
                listingsApi.deleteListingImage(authorize, listingId, payload.imageId)
            }
        }
        val apiException = result.exceptionOrNull() as? ApiException
        if (result.isFailure && apiException?.statusCode != 404) {
            result.getOrThrow()
        }
        store.markListingSynced(listingId)
    }

    private suspend fun syncUpdateImagesOrder(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val payload = store.decodeImagesOrderPayload(operation)
        apiClient.request { authorize ->
            listingsApi.updateListingImagesOrder(
                authorize = authorize,
                listingId = listingId,
                request = UpdateListingImagesOrderRequestDto(payload.imageIds),
            )
        }
        store.markListingSynced(listingId)
    }

    private suspend fun syncAddFavourite(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val result = runCatching {
            apiClient.request { authorize ->
                favouritesApi.addFavourite(authorize, listingId)
            }
        }
        val apiException = result.exceptionOrNull() as? ApiException
        if (result.isFailure && apiException?.statusCode != 409) {
            result.getOrThrow()
        }
        store.setFavouriteSynced(listingId)
    }

    private suspend fun syncRemoveFavourite(operation: kupio.mobile.core.offline.db.PendingSyncOperationEntity) {
        val listingId = operation.resolvedListingId()
        val result = runCatching {
            apiClient.request { authorize ->
                favouritesApi.removeFavourite(authorize, listingId)
            }
        }
        val apiException = result.exceptionOrNull() as? ApiException
        if (result.isFailure && apiException?.statusCode != 404) {
            result.getOrThrow()
        }
        store.setFavouriteSynced(listingId)
    }

    private suspend fun kupio.mobile.core.offline.db.PendingSyncOperationEntity.resolvedListingId(): String =
        requireNotNull(listingId?.let { store.resolveListingId(it) }) {
            "Pending operation $id has no synced listing id."
        }

    private companion object {
        const val DefaultBatchSize = 50
    }
}

class OfflineSyncFailedException(
    cause: Throwable,
) : Exception("One or more offline operations failed to sync.", cause)
