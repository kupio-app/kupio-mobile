package kupio.mobile.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

private val ThemeModeKey = stringPreferencesKey("theme_mode")

internal const val KupioPreferencesFileName = "kupio.preferences_pb"

fun createPreferencesDataStore(
    producePath: () -> String,
): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )
}

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {
    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorageValue(preferences[ThemeModeKey])
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.storageValue
        }
    }

    override fun chatLastSeenEpochMillis(conversationId: String): Flow<Long?> =
        dataStore.data.map { it[longPreferencesKey("chat_seen_$conversationId")] }

    override suspend fun markChatSeen(conversationId: String, epochMillis: Long) {
        dataStore.edit { it[longPreferencesKey("chat_seen_$conversationId")] = epochMillis }
    }
}
