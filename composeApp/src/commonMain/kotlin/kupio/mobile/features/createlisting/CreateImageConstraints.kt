package kupio.mobile.features.createlisting

internal val AllowedListingImageMimeTypes = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
)

internal const val UnsupportedListingImageMessage = "Only JPG, PNG, or WEBP photos can be uploaded."

internal fun isSupportedListingImageMimeType(mimeType: String): Boolean =
    mimeType.lowercase() in AllowedListingImageMimeTypes

internal data class NormalizedListingImage(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NormalizedListingImage) return false

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

internal expect fun normalizeListingImage(
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
): NormalizedListingImage

internal fun String.withJpegExtension(): String {
    val baseName = substringBeforeLast('.', missingDelimiterValue = this)
        .ifBlank { "listing-photo" }
    return "$baseName.jpg"
}
