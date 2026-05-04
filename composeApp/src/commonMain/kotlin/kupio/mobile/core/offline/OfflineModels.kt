package kupio.mobile.core.offline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.listings.data.remote.ListingRequestDto
import kupio.mobile.features.listings.data.remote.ListingStatusDto

enum class OfflineSyncState {
    SYNCED,
    PENDING,
    SYNCING,
    FAILED,
}

enum class PendingOperationType {
    CREATE_LISTING,
    UPDATE_LISTING,
    UPDATE_LISTING_STATUS,
    UPLOAD_LISTING_IMAGES,
    DELETE_LISTING_IMAGE,
    UPDATE_LISTING_IMAGES_ORDER,
    ADD_FAVOURITE,
    REMOVE_FAVOURITE,
}

@Serializable
data class CachedListingImage(
    val id: String,
    val url: String,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
data class CreateListingOperationPayload(
    val request: ListingRequestDto,
)

@Serializable
data class UpdateListingOperationPayload(
    val request: ListingRequestDto,
)

@Serializable
data class UpdateListingStatusOperationPayload(
    val status: ListingStatusDto,
)

@Serializable
data class DeleteListingImageOperationPayload(
    val imageId: String,
)

@Serializable
data class UpdateListingImagesOrderOperationPayload(
    val imageIds: List<String>,
)

@Serializable
data class FavouriteOperationPayload(
    val listingId: String,
)
