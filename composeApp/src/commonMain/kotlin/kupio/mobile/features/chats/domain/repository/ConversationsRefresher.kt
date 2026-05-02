package kupio.mobile.features.chats.domain.repository

interface ConversationsRefresher {
    suspend fun refresh()
}
