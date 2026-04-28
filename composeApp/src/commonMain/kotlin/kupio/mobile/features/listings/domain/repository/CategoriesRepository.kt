package kupio.mobile.features.listings.domain.repository

import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition

interface CategoriesRepository {
    suspend fun getRootCategories(limit: Int = 20): List<Category>

    suspend fun getSubcategories(
        categoryId: Int,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): List<Category>

    suspend fun getCategoryFilters(
        categoryId: Int,
        forceRefresh: Boolean = false,
    ): List<FilterDefinition>
}
