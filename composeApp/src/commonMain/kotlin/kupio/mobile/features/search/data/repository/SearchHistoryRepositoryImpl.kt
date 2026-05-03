package kupio.mobile.features.search.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kupio.mobile.core.datetime.nowEpochMillis
import kupio.mobile.features.search.domain.model.RecentSearch
import kupio.mobile.features.search.domain.repository.SearchHistoryRepository

private val SearchHistoryKey = stringPreferencesKey("search_history")
private const val MAX_RECENT_SEARCHES = 10

class SearchHistoryRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SearchHistoryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getRecentSearches(): Flow<List<RecentSearch>> =
        dataStore.data.map { prefs ->
            prefs[SearchHistoryKey]
                ?.let { runCatching { json.decodeFromString<List<RecentSearch>>(it) }.getOrNull() }
                ?: emptyList()
        }

    override suspend fun addSearch(query: String, categoryId: Int?, categoryName: String?) {
        if (query.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[SearchHistoryKey]
                ?.let { runCatching { json.decodeFromString<List<RecentSearch>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = listOf(
                RecentSearch(
                    query = query.trim(),
                    categoryId = categoryId,
                    categoryName = categoryName,
                    timestamp = nowEpochMillis(),
                ),
            ) + current.filter { it.query != query.trim() }
            prefs[SearchHistoryKey] = json.encodeToString(updated.take(MAX_RECENT_SEARCHES))
        }
    }

    override suspend fun removeSearch(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[SearchHistoryKey]
                ?.let { runCatching { json.decodeFromString<List<RecentSearch>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[SearchHistoryKey] = json.encodeToString(current.filter { it.query != query })
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.remove(SearchHistoryKey) }
    }
}
