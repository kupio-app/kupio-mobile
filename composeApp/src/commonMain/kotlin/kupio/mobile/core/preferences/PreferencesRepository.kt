package kupio.mobile.core.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val pushToken: Flow<String?>

    suspend fun setThemeMode(mode: ThemeMode)

    fun chatLastSeenEpochMillis(conversationId: String): Flow<Long?>

    suspend fun markChatSeen(conversationId: String, epochMillis: Long)

    fun savePushToken(token: String)
}
