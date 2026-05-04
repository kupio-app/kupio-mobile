package kupio.mobile.core.offline.files

import android.content.Context
import java.io.File

class AndroidOfflineFileStore(
    context: Context,
) : OfflineFileStore {
    private val imagesDir = File(context.filesDir, "offline_listing_images")

    override suspend fun saveListingImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        imagesDir.mkdirs()
        val safeName = fileName.safeFileName()
        val file = File(imagesDir, "${currentTimeMillis()}-$safeName")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    override suspend fun read(path: String): ByteArray = File(path).readBytes()

    override suspend fun delete(path: String) {
        File(path).delete()
    }
}

private fun String.safeFileName(): String =
    map { character ->
        when {
            character == '/' || character == '\\' -> '_'
            character.code < 0x20 || character.code == 0x7F -> '_'
            else -> character
        }
    }.joinToString("").ifBlank { "image" }

private fun currentTimeMillis(): Long = System.currentTimeMillis()
