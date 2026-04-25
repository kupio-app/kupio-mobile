package kupio.mobile.features.listings.domain.repository

import kupio.mobile.features.listings.domain.model.Category

interface CategoriesRepository {
    suspend fun getRootCategories(limit: Int = 20): List<Category>
}
