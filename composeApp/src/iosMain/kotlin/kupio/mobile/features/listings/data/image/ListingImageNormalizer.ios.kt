package kupio.mobile.features.listings.data.image

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

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

    val image = UIImage(data = bytes.toNSData())
    val jpegData = UIImageJPEGRepresentation(image, 0.9)
        ?: return NormalizedListingImage(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )

    return NormalizedListingImage(
        fileName = fileName.withJpegExtension(),
        mimeType = "image/jpeg",
        bytes = jpegData.toByteArray(),
    )
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return NSData.create(bytes = null, length = 0u)
    }

    return usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isEmpty()) return result

    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length.convert())
    }
    return result
}
