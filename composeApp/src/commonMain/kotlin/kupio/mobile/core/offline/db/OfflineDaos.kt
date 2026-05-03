package kupio.mobile.core.offline.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LocalListingsDao {
    @Upsert
    suspend fun upsert(listing: LocalListingEntity)

    @Upsert
    suspend fun upsertAll(listings: List<LocalListingEntity>)

    @Query("SELECT * FROM local_listings WHERE id = :id OR serverId = :id LIMIT 1")
    suspend fun getByIdOrServerId(id: String): LocalListingEntity?

    @Query("SELECT * FROM local_listings WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): LocalListingEntity?

    @Query("SELECT * FROM local_listings ORDER BY lastModifiedAtMs DESC")
    suspend fun getAll(): List<LocalListingEntity>

    @Query("SELECT * FROM local_listings WHERE userId = :userId OR syncState != 'SYNCED' ORDER BY lastModifiedAtMs DESC")
    suspend fun getOwned(userId: String): List<LocalListingEntity>

    @Query("DELETE FROM local_listings WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CachedFavouritesDao {
    @Upsert
    suspend fun upsert(favourite: CachedFavouriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(favourites: List<CachedFavouriteEntity>)

    @Query("DELETE FROM cached_favourites")
    suspend fun clear()

    @Query("SELECT * FROM cached_favourites WHERE desired = 1")
    suspend fun getDesired(): List<CachedFavouriteEntity>

    @Query("SELECT * FROM cached_favourites WHERE listingId = :listingId LIMIT 1")
    suspend fun get(listingId: String): CachedFavouriteEntity?

    @Query("UPDATE cached_favourites SET synced = :synced, updatedAtMs = :updatedAtMs WHERE listingId = :listingId")
    suspend fun setSynced(listingId: String, synced: Boolean, updatedAtMs: Long)
}

@Dao
interface PendingSyncDao {
    @Insert
    suspend fun insert(operation: PendingSyncOperationEntity): Long

    @Upsert
    suspend fun upsert(operation: PendingSyncOperationEntity)

    @Query("SELECT * FROM pending_sync_operations WHERE state IN ('PENDING', 'FAILED') ORDER BY createdAtMs ASC, id ASC LIMIT :limit")
    suspend fun getRunnable(limit: Int): List<PendingSyncOperationEntity>

    @Query("SELECT * FROM pending_sync_operations WHERE listingId = :listingId AND type = :type AND state IN ('PENDING', 'FAILED') ORDER BY createdAtMs DESC LIMIT 1")
    suspend fun getLatestRunnableForListing(listingId: String, type: String): PendingSyncOperationEntity?

    @Query("SELECT * FROM pending_sync_operations WHERE listingId = :listingId AND type = 'CREATE_LISTING' AND state IN ('PENDING', 'FAILED', 'SYNCING') ORDER BY createdAtMs ASC LIMIT 1")
    suspend fun getCreateForListing(listingId: String): PendingSyncOperationEntity?

    @Query("UPDATE pending_sync_operations SET state = :state, attempts = :attempts, lastError = :lastError, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateState(id: Long, state: String, attempts: Int, lastError: String?, updatedAtMs: Long)

    @Query("UPDATE pending_sync_operations SET state = 'PENDING', updatedAtMs = :updatedAtMs WHERE state = 'SYNCING'")
    suspend fun resetStuckSyncing(updatedAtMs: Long)

    @Query("DELETE FROM pending_sync_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_operations WHERE listingId = :listingId AND type = :type AND id != :keepId AND state IN ('PENDING', 'FAILED')")
    suspend fun deleteOtherRunnableForListing(listingId: String, type: String, keepId: Long)

    @Query("UPDATE pending_sync_operations SET listingId = :newListingId, updatedAtMs = :updatedAtMs WHERE listingId = :oldListingId")
    suspend fun replaceListingId(oldListingId: String, newListingId: String, updatedAtMs: Long)
}

@Dao
interface PendingListingImagesDao {
    @Insert
    suspend fun insertAll(images: List<PendingListingImageEntity>)

    @Query("SELECT * FROM pending_listing_images WHERE operationId = :operationId ORDER BY sortOrder ASC")
    suspend fun getForOperation(operationId: Long): List<PendingListingImageEntity>

    @Query("DELETE FROM pending_listing_images WHERE operationId = :operationId")
    suspend fun deleteForOperation(operationId: Long)
}
