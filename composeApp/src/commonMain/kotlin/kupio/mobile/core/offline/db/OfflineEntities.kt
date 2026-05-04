package kupio.mobile.core.offline.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_listings",
    indices = [
        Index("serverId"),
        Index("userId"),
        Index("syncState"),
    ],
)
data class LocalListingEntity(
    @PrimaryKey val id: String,
    val serverId: String?,
    val title: String,
    val description: String,
    val price: Int,
    val currency: String,
    val status: String,
    val primaryImageUrl: String?,
    val imagesJson: String,
    val createdAt: String,
    val updatedAt: String?,
    val userId: String,
    val categoryId: Int,
    val categoryName: String,
    val seenCount: Int,
    val phone: String?,
    val contactName: String?,
    val isCallsDisabled: Boolean,
    val isFree: Boolean,
    val isTradable: Boolean,
    val customFiltersJson: String,
    val syncState: String,
    val lastError: String?,
    val lastModifiedAtMs: Long,
)

@Entity(tableName = "cached_favourites")
data class CachedFavouriteEntity(
    @PrimaryKey val listingId: String,
    val desired: Boolean,
    val synced: Boolean,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "pending_sync_operations",
    indices = [
        Index("type"),
        Index("listingId"),
        Index("state"),
        Index("createdAtMs"),
    ],
)
data class PendingSyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val listingId: String?,
    val payloadJson: String,
    val state: String,
    val attempts: Int,
    val lastError: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "pending_listing_images",
    indices = [
        Index("operationId"),
    ],
)
data class PendingListingImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationId: Long,
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val sortOrder: Int,
)

@Entity(tableName = "cached_authenticated_user")
data class CachedAuthenticatedUserEntity(
    @PrimaryKey val id: String,
    val username: String?,
    val displayName: String?,
    val email: String,
    val role: String,
    val needsUsername: Boolean,
    val balance: Int,
    val avatarUrl: String?,
    val createdAt: String?,
)

@Entity(
    tableName = "cached_categories",
    indices = [
        Index("parentId"),
        Index("depth"),
    ],
)
data class CachedCategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val iconSlug: String?,
    val depth: Int,
    val parentId: Int?,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "cached_category_filters",
    indices = [
        Index("categoryId"),
    ],
)
data class CachedCategoryFilterEntity(
    @PrimaryKey val id: Int,
    val categoryId: Int,
    val slug: String,
    val label: String,
    val type: String,
    val optionsJson: String,
    val isRequired: Boolean,
    val displayOrder: Int,
    val updatedAtMs: Long,
)
