package kupio.mobile.core.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    fun chatLastSeenEpochMillis(conversationId: String): Flow<Long?>

    suspend fun markChatSeen(conversationId: String, epochMillis: Long)
}
