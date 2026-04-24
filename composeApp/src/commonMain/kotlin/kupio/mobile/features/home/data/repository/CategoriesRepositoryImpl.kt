package kupio.mobile.features.home.data.repository

import kupio.mobile.features.home.data.remote.CategoriesApi
import kupio.mobile.features.home.data.remote.toDomain
import kupio.mobile.features.home.domain.model.Category
import kupio.mobile.features.home.domain.repository.CategoriesRepository

class CategoriesRepositoryImpl(
    private val categoriesApi: CategoriesApi,
) : CategoriesRepository {
    override suspend fun getRootCategories(limit: Int): List<Category> =
        categoriesApi.getCategories(depth = 0, limit = limit)
            .map { it.toDomain() }
}
