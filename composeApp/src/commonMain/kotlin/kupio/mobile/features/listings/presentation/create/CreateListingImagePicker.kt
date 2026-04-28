package kupio.mobile.features.listings.presentation.create

import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.extensions.loadImageBitmap
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult
import kupio.mobile.features.listings.data.image.normalizeListingImage
import kotlin.random.Random

internal fun List<PhotoResult>.toSelectedImages(): List<SelectedListingImage> =
    mapNotNull { photo ->
        val bytes = photo.loadBytes()
        if (bytes.isEmpty()) return@mapNotNull null
        val fileName = photo.fileName ?: "listing-photo-${Random.nextLong().toString().takeLast(6)}.jpg"
        val mimeType = photo.mimeType ?: photo.fileName.inferListingImageMimeType()
        val normalizedImage = normalizeListingImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
        SelectedListingImage(
            id = photo.uri.ifBlank { Random.nextLong().toString() },
            fileName = normalizedImage.fileName,
            mimeType = normalizedImage.mimeType,
            bytes = normalizedImage.bytes,
            previewBitmap = photo.loadImageBitmap(),
        )
    }

private fun String?.inferListingImageMimeType(): String = when (this?.substringAfterLast('.', missingDelimiterValue = "")
    ?.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    "heic", "heif" -> "image/heic"
    else -> "application/octet-stream"
}
