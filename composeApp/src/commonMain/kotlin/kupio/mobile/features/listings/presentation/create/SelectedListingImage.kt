package kupio.mobile.features.listings.presentation.create

import androidx.compose.ui.graphics.ImageBitmap

data class SelectedListingImage(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val previewBitmap: ImageBitmap? = null,
) {
    val sizeBytes: Int get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedListingImage) return false

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
