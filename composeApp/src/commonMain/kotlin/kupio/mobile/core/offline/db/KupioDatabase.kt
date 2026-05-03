package kupio.mobile.core.offline.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        LocalListingEntity::class,
        CachedFavouriteEntity::class,
        PendingSyncOperationEntity::class,
        PendingListingImageEntity::class,
    ],
    version = 1,
)
@ConstructedBy(KupioDatabaseConstructor::class)
abstract class KupioDatabase : RoomDatabase() {
    abstract fun listingsDao(): LocalListingsDao
    abstract fun favouritesDao(): CachedFavouritesDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun pendingImagesDao(): PendingListingImagesDao
}

@Suppress("KotlinNoActualForExpect")
expect object KupioDatabaseConstructor : RoomDatabaseConstructor<KupioDatabase> {
    override fun initialize(): KupioDatabase
}

fun createKupioDatabase(
    builder: RoomDatabase.Builder<KupioDatabase>,
): KupioDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
