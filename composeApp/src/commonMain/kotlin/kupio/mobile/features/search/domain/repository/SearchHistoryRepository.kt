package kupio.mobile.features.search.domain.repository

import kotlinx.coroutines.flow.Flow
import kupio.mobile.features.search.domain.model.RecentSearch

interface SearchHistoryRepository {
    fun getRecentSearches(): Flow<List<RecentSearch>>
    suspend fun addSearch(query: String, categoryId: Int? = null, categoryName: String? = null)
    suspend fun removeSearch(query: String)
    suspend fun clearAll()
}
