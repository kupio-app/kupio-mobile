package kupio.mobile.features.listings.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

internal actual fun normalizeListingImage(
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
): NormalizedListingImage {
    if (isSupportedListingImageMimeType(mimeType)) {
        return NormalizedListingImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
    }

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: return NormalizedListingImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )

    return try {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        NormalizedListingImage(
            fileName = fileName.withJpegExtension(),
            mimeType = "image/jpeg",
            bytes = output.toByteArray(),
        )
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
