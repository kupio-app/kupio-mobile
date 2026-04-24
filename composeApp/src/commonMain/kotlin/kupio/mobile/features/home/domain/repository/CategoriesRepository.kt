package kupio.mobile.features.home.domain.repository

import kupio.mobile.features.home.domain.model.Category

interface CategoriesRepository {
    suspend fun getRootCategories(limit: Int = 20): List<Category>
}
