package kupio.mobile.features.chats.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kupio.mobile.core.datetime.nowEpochMillis
import kupio.mobile.core.datetime.toTimeLabel
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.data.remote.UserApi
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.domain.model.ConversationData
import kupio.mobile.features.chats.domain.model.ListingSummary
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ConversationsStore(
    private val chatsRepository: ChatsRepository,
    private val listingsRepository: ListingsRepository,
    private val userApi: UserApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val preferences: PreferencesRepository,
    private val sessionManager: AuthSessionManager,
) {
    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _unreadCount = MutableStateFlow(0)

    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun observeConversation(id: String): Flow<ChatSummary?> =
        _chats.map { list -> list.find { it.id == id } }

    suspend fun refresh() {
        _isLoading.value = true
        _error.value = null
        runCatching {
            coroutineScope {
                val buyingDeferred = async {
                    chatsRepository.listConversations(ChatRole.BUYING).map { it to ChatRole.BUYING }
                }
                val sellingDeferred = async {
                    chatsRepository.listConversations(ChatRole.SELLING).map { it to ChatRole.SELLING }
                }
                val merged = (buyingDeferred.await() + sellingDeferred.await())
                    .sortedByDescending { (conv, _) -> conv.createdAt }

                val currentUserId = currentUserId()
                val summaries = merged.map { (conv, role) ->
                    async { buildSummary(conv, role) }
                }.awaitAll()

                _chats.value = summaries
                _unreadCount.value = summaries.sumOf { it.unreadCount }
            }
        }.onFailure { t ->
            if (t is CancellationException) throw t
            _error.value = t.message ?: "Unknown error"
        }
        _isLoading.value = false
    }

    suspend fun refreshUnreadCount() {
        runCatching { chatsRepository.getUnreadCount() }
            .onSuccess { count -> _unreadCount.value = count }
    }

    suspend fun markSeen(conversationId: String) {
        preferences.markChatSeen(conversationId, nowEpochMillis())
        _chats.update { list ->
            val prev = list.find { it.id == conversationId }?.unreadCount ?: 0
            _unreadCount.update { (it - prev).coerceAtLeast(0) }
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
    }

    fun updatePreview(conversationId: String, preview: String, timeLabel: String) {
        _chats.update { list ->
            list.map { chat ->
                if (chat.id == conversationId) chat.copy(
                    lastMessagePreview = preview,
                    lastMessageTimeLabel = timeLabel,
                ) else chat
            }
        }
    }

    private suspend fun buildSummary(
        conv: ConversationData,
        role: ChatRole
    ): ChatSummary {
        val otherPartyId = if (role == ChatRole.BUYING) conv.sellerId else conv.buyerId

        val (listing, participant) = coroutineScope {
            val listingDeferred = async {
                runCatching { listingsRepository.getListing(conv.listingId) }.getOrNull()
            }
            val userDeferred = async {
                runCatching {
                    authenticatedApiClient.request { authorize ->
                        userApi.getUserById(authorize, otherPartyId)
                    }
                }.getOrNull()
            }
            listingDeferred.await() to userDeferred.await()
        }

        val participantLabel = participant?.displayName
            ?: participant?.username?.let { "User $it" }
            ?: "User ${otherPartyId.take(8)}"
        val participantInitials = participantLabel
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { otherPartyId.take(2).uppercase() }

        return ChatSummary(
            id = conv.id,
            role = role,
            participantId = otherPartyId,
            participantLabel = participantLabel,
            participantInitials = participantInitials,
            listing = listing?.toSummary() ?: ListingSummary(
                id = conv.listingId,
                title = "Listing",
                priceFormatted = "",
                placeholderSeed = conv.listingId.hashCode(),
            ),
            lastMessagePreview = conv.lastMessagePreview,
            lastMessageTimeLabel = conv.createdAt.toTimeLabel(),
            unreadCount = conv.unreadCount,
        )
    }

    private fun currentUserId() =
        (sessionManager.sessionState.value as? SessionState.SignedIn)?.user?.id.orEmpty()
}