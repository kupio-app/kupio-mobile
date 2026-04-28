package kupio.mobile.features.listings.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kupio.mobile.features.listings.data.remote.CategoriesApi
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.repository.CategoriesRepository

class CategoriesRepositoryImpl(
    private val categoriesApi: CategoriesApi,
) : CategoriesRepository {
    private val filtersCache = mutableMapOf<Int, List<FilterDefinition>>()
    private val filtersCacheMutex = Mutex()
    private val subcategoriesCache = mutableMapOf<Int, List<Category>>()
    private val subcategoriesCacheMutex = Mutex()

    override suspend fun getRootCategories(limit: Int): List<Category> =
        categoriesApi.getCategories(depth = 0, limit = limit)
            .map { it.toDomain() }

    override suspend fun getSubcategories(
        categoryId: Int,
        limit: Int,
        forceRefresh: Boolean,
    ): List<Category> {
        if (!forceRefresh) {
            subcategoriesCacheMutex.withLock {
                subcategoriesCache[categoryId]
            }?.let { return it }
        }

        val subcategories = categoriesApi.getSubcategories(categoryId = categoryId, limit = limit)
            .map { it.toDomain() }
        subcategoriesCacheMutex.withLock {
            subcategoriesCache[categoryId] = subcategories
        }
        return subcategories
    }

    override suspend fun getCategoryFilters(
        categoryId: Int,
        forceRefresh: Boolean,
    ): List<FilterDefinition> {
        if (!forceRefresh) {
            filtersCacheMutex.withLock {
                filtersCache[categoryId]
            }?.let { return it }
        }

        val filters = categoriesApi.getCategoryFilters(categoryId)
            .map { it.toDomain() }
            .sortedBy { it.displayOrder }
        filtersCacheMutex.withLock {
            filtersCache[categoryId] = filters
        }
        return filters
    }
}
