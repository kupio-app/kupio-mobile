package kupio.mobile.features.listings.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kupio.mobile.core.offline.db.CachedCategoryEntity
import kupio.mobile.core.offline.db.CachedCategoryFilterEntity
import kupio.mobile.core.offline.db.KupioDatabase
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterOptions
import kupio.mobile.features.listings.domain.model.FilterType
import kotlin.time.Clock

class CategoriesCacheStore(
    database: KupioDatabase,
) {
    private val categoriesDao = database.categoriesDao()
    private val filtersDao = database.categoryFiltersDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRootCategories(limit: Int): List<Category> =
        categoriesDao.getRootCategories(limit).map { it.toDomain() }

    suspend fun replaceRootCategories(categories: List<Category>) {
        val now = nowMs()
        categoriesDao.deleteRootCategories()
        categoriesDao.upsertAll(categories.map { it.toEntity(now) })
    }

    suspend fun getSubcategories(categoryId: Int, limit: Int): List<Category> =
        categoriesDao.getSubcategories(categoryId, limit).map { it.toDomain() }

    suspend fun replaceSubcategories(categoryId: Int, categories: List<Category>) {
        val now = nowMs()
        categoriesDao.deleteSubcategories(categoryId)
        categoriesDao.upsertAll(categories.map { it.toEntity(now) })
    }

    suspend fun getCategoryFilters(categoryId: Int): List<FilterDefinition> =
        filtersDao.getForCategory(categoryId).map { it.toDomain() }

    suspend fun replaceCategoryFilters(categoryId: Int, filters: List<FilterDefinition>) {
        val now = nowMs()
        filtersDao.deleteForCategory(categoryId)
        filtersDao.upsertAll(filters.map { it.toEntity(now) })
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun Category.toEntity(updatedAtMs: Long): CachedCategoryEntity =
        CachedCategoryEntity(
            id = id,
            name = name,
            iconSlug = iconSlug,
            depth = depth,
            parentId = parentId,
            updatedAtMs = updatedAtMs,
        )

    private fun CachedCategoryEntity.toDomain(): Category =
        Category(
            id = id,
            name = name,
            iconSlug = iconSlug,
            depth = depth,
            parentId = parentId,
        )

    private fun FilterDefinition.toEntity(updatedAtMs: Long): CachedCategoryFilterEntity =
        CachedCategoryFilterEntity(
            id = id,
            categoryId = categoryId,
            slug = slug,
            label = label,
            type = type.name,
            optionsJson = json.encodeToString(options.toCached()),
            isRequired = isRequired,
            displayOrder = displayOrder,
            updatedAtMs = updatedAtMs,
        )

    private fun CachedCategoryFilterEntity.toDomain(): FilterDefinition =
        FilterDefinition(
            id = id,
            categoryId = categoryId,
            slug = slug,
            label = label,
            type = FilterType.valueOf(type),
            options = json.decodeFromString<CachedFilterOptions>(optionsJson).toDomain(),
            isRequired = isRequired,
            displayOrder = displayOrder,
        )
}

@Serializable
private data class CachedFilterOptions(
    val values: List<String> = emptyList(),
    val min: Double? = null,
    val max: Double? = null,
)

private fun FilterOptions.toCached(): CachedFilterOptions =
    CachedFilterOptions(values = values, min = min, max = max)

private fun CachedFilterOptions.toDomain(): FilterOptions =
    FilterOptions(values = values, min = min, max = max)
