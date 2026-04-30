package kupio.mobile.features.listings.presentation.form

import androidx.compose.ui.graphics.ImageBitmap
import kupio.mobile.features.listings.domain.model.ListingImageUpload

sealed interface ListingFormImage {
    val id: String
    val fileName: String
    val previewBitmap: ImageBitmap?
    val imageUrl: String?
}

data class LocalListingImage(
    override val id: String,
    override val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    override val previewBitmap: ImageBitmap? = null,
) : ListingFormImage {
    override val imageUrl: String? = null
    val sizeBytes: Int get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalListingImage) return false

        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class RemoteListingImage(
    override val id: String,
    val imageId: String,
    override val imageUrl: String,
    val sortOrder: Int,
) : ListingFormImage {
    override val fileName: String = imageId
    override val previewBitmap: ImageBitmap? = null
}

fun LocalListingImage.toUpload(): ListingImageUpload = ListingImageUpload(
    fileName = fileName,
    mimeType = mimeType,
    bytes = bytes,
)
