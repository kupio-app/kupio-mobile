package kupio.mobile.core.offline.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite

class IosOfflineFileStore : OfflineFileStore {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveListingImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val directory = imagesDirectory()
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val path = "$directory/${currentTimeMillis()}-${fileName.safeFileName()}"
        val file = fopen(path, "wb") ?: error("Could not write offline image.")
        try {
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
                }
            }
        } finally {
            fclose(file)
        }
        return path
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun read(path: String): ByteArray {
        val file = fopen(path, "rb") ?: error("Offline image is unavailable.")
        try {
            fseek(file, 0, SEEK_END)
            val size = ftell(file).toInt()
            fseek(file, 0, SEEK_SET)
            val bytes = ByteArray(size)
            if (size > 0) {
                bytes.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1.convert(), size.convert(), file)
                }
            }
            return bytes
        } finally {
            fclose(file)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun delete(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun imagesDirectory(): String {
    val applicationSupportDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val basePath = requireNotNull(applicationSupportDirectory?.path) {
        "Application Support path is unavailable."
    }
    return "$basePath/offline_listing_images"
}

private fun String.safeFileName(): String =
    map { character ->
        when {
            character == '/' || character == '\\' -> '_'
            character.code < 0x20 || character.code == 0x7F -> '_'
            else -> character
        }
    }.joinToString("").ifBlank { "image" }

private fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
