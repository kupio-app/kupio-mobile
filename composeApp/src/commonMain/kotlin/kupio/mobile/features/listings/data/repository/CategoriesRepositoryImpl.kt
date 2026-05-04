package kupio.mobile.features.listings.data.repository

import kupio.mobile.features.listings.data.remote.CategoriesApi
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.repository.CategoriesRepository

class CategoriesRepositoryImpl(
    private val categoriesApi: CategoriesApi,
    private val cacheStore: CategoriesCacheStore,
) : CategoriesRepository {
    override suspend fun getRootCategories(limit: Int): List<Category> =
        runCatching {
            categoriesApi.getCategories(depth = 0, limit = limit)
                .map { it.toDomain() }
                .also { cacheStore.replaceRootCategories(it) }
        }.getOrElse {
            cacheStore.getRootCategories(limit)
        }

    override suspend fun getSubcategories(
        categoryId: Int,
        limit: Int,
        forceRefresh: Boolean,
    ): List<Category> {
        if (!forceRefresh) {
            cacheStore.getSubcategories(categoryId, limit)
                .takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        return runCatching {
            categoriesApi.getSubcategories(categoryId = categoryId, limit = limit)
                .map { it.toDomain() }
                .also { cacheStore.replaceSubcategories(categoryId, it) }
        }.getOrElse {
            cacheStore.getSubcategories(categoryId, limit)
        }
    }

    override suspend fun getCategoryFilters(
        categoryId: Int,
        forceRefresh: Boolean,
    ): List<FilterDefinition> {
        if (!forceRefresh) {
            cacheStore.getCategoryFilters(categoryId)
                .takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        return runCatching {
            categoriesApi.getCategoryFilters(categoryId)
                .map { it.toDomain() }
                .sortedBy { it.displayOrder }
                .also { cacheStore.replaceCategoryFilters(categoryId, it) }
        }.getOrElse {
            cacheStore.getCategoryFilters(categoryId)
        }
    }
}
