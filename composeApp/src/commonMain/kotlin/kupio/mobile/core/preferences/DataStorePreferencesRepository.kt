package kupio.mobile.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorageValue(preferences[ThemeModeKey])
    }

    override val pushToken: Flow<String?> = dataStore.data.map {
        preferences -> preferences[stringPreferencesKey("push_token")]
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

    override fun savePushToken(token: String) {
        repositoryScope.launch {
            dataStore.edit { it[stringPreferencesKey("push_token")] = token }
        }
    }
}
