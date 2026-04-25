package kupio.mobile.features.listings.data.repository

import kupio.mobile.features.listings.data.remote.CategoriesApi
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.repository.CategoriesRepository

class CategoriesRepositoryImpl(
    private val categoriesApi: CategoriesApi,
) : CategoriesRepository {
    override suspend fun getRootCategories(limit: Int): List<Category> =
        categoriesApi.getCategories(depth = 0, limit = limit)
            .map { it.toDomain() }
}
