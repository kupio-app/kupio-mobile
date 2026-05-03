package kupio.mobile.core.offline.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getKupioDatabaseBuilder(): RoomDatabase.Builder<KupioDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val basePath = requireNotNull(directory?.path) {
        "Application Support path is unavailable."
    }
    return Room.databaseBuilder<KupioDatabase>(
        name = "$basePath/kupio_offline.db",
    )
}
