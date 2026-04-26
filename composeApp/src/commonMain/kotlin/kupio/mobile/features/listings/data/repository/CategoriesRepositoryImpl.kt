package kupio.mobile.features.listings.data.repository

import kupio.mobile.features.listings.data.remote.CategoriesApi
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.repository.CategoriesRepository

class CategoriesRepositoryImpl(
    private val categoriesApi: CategoriesApi,
) : CategoriesRepository {
    private val filtersCache = mutableMapOf<Int, List<FilterDefinition>>()

    override suspend fun getRootCategories(limit: Int): List<Category> =
        categoriesApi.getCategories(depth = 0, limit = limit)
            .map { it.toDomain() }

    override suspend fun getCategoryFilters(
        categoryId: Int,
        forceRefresh: Boolean,
    ): List<FilterDefinition> {
        if (!forceRefresh) {
            filtersCache[categoryId]?.let { return it }
        }

        return categoriesApi.getCategoryFilters(categoryId)
            .map { it.toDomain() }
            .sortedBy { it.displayOrder }
            .also { filtersCache[categoryId] = it }
    }
}
