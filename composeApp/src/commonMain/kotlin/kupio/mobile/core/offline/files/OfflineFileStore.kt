package kupio.mobile.core.offline.files

interface OfflineFileStore {
    suspend fun saveListingImage(
        fileName: String,
        bytes: ByteArray,
    ): String

    suspend fun read(path: String): ByteArray

    suspend fun delete(path: String)
}
