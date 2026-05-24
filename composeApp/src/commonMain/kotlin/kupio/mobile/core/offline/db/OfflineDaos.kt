package kupio.mobile.core.offline.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM local_listings WHERE userId = :userId ORDER BY lastModifiedAtMs DESC")
    suspend fun getOwned(userId: String): List<LocalListingEntity>

    @Query("SELECT * FROM local_listings WHERE id IN (:ids) OR serverId IN (:ids)")
    suspend fun getByIdsOrServerIds(ids: List<String>): List<LocalListingEntity>

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

    @Query("SELECT * FROM cached_favourites")
    suspend fun getAll(): List<CachedFavouriteEntity>

    @Query("SELECT * FROM cached_favourites WHERE synced = 0")
    suspend fun getUnsynced(): List<CachedFavouriteEntity>

    @Query("SELECT * FROM cached_favourites WHERE listingId = :listingId LIMIT 1")
    suspend fun get(listingId: String): CachedFavouriteEntity?

    @Query("UPDATE cached_favourites SET synced = :synced, updatedAtMs = :updatedAtMs WHERE listingId = :listingId")
    suspend fun setSynced(listingId: String, synced: Boolean, updatedAtMs: Long)

    @Query("SELECT listingId FROM cached_favourites WHERE desired = 1")
    fun observeDesiredIds(): Flow<List<String>>

    @Transaction
    suspend fun clearAndReplaceAll(favourites: List<CachedFavouriteEntity>) {
        clear()
        replaceAll(favourites)
    }
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

@Dao
interface CachedAuthenticatedUserDao {
    @Upsert
    suspend fun upsert(user: CachedAuthenticatedUserEntity)

    @Query("SELECT * FROM cached_authenticated_user LIMIT 1")
    suspend fun get(): CachedAuthenticatedUserEntity?

    @Query("DELETE FROM cached_authenticated_user")
    suspend fun clear()
}

@Dao
interface CachedCategoriesDao {
    @Upsert
    suspend fun upsertAll(categories: List<CachedCategoryEntity>)

    @Query("SELECT * FROM cached_categories WHERE depth = 0 ORDER BY name ASC LIMIT :limit")
    suspend fun getRootCategories(limit: Int): List<CachedCategoryEntity>

    @Query("SELECT * FROM cached_categories WHERE parentId = :categoryId ORDER BY name ASC LIMIT :limit")
    suspend fun getSubcategories(categoryId: Int, limit: Int): List<CachedCategoryEntity>

    @Query("DELETE FROM cached_categories WHERE depth = 0")
    suspend fun deleteRootCategories()

    @Query("DELETE FROM cached_categories WHERE parentId = :categoryId")
    suspend fun deleteSubcategories(categoryId: Int)
}

@Dao
interface CachedCategoryFiltersDao {
    @Upsert
    suspend fun upsertAll(filters: List<CachedCategoryFilterEntity>)

    @Query("SELECT * FROM cached_category_filters WHERE categoryId = :categoryId ORDER BY displayOrder ASC")
    suspend fun getForCategory(categoryId: Int): List<CachedCategoryFilterEntity>

    @Query("DELETE FROM cached_category_filters WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Int)
}
