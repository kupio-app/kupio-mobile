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
import kupio.mobile.core.di.SessionCleaner
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
import kupio.mobile.features.chats.domain.repository.ConversationsRefresher
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ConversationsStore(
    private val chatsRepository: ChatsRepository,
    private val listingsRepository: ListingsRepository,
    private val userApi: UserApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val preferences: PreferencesRepository,
    sessionCleaner: SessionCleaner,
) : ConversationsRefresher {
    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _unreadCount = MutableStateFlow(0)
    private val _hasMore = MutableStateFlow(false)
    private var nextCursorBuying: String? = null
    private var nextCursorSelling: String? = null

    init {
        sessionCleaner.register(::clearState)
    }

    private fun clearState() {
        _chats.value = emptyList()
        _unreadCount.value = 0
        _error.value = null
        _isLoading.value = false
        _isLoadingMore.value = false
        _hasMore.value = false
        nextCursorBuying = null
        nextCursorSelling = null
    }

    val chats: StateFlow<List<ChatSummary>> = _chats.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    fun observeConversation(id: String): Flow<ChatSummary?> =
        _chats.map { list -> list.find { it.id == id } }

    override suspend fun refresh() {
        _isLoading.value = true
        _error.value = null
        nextCursorBuying = null
        nextCursorSelling = null
        try {
            runCatching {
                coroutineScope {
                    val buyingDeferred = async { chatsRepository.listConversationsPage(ChatRole.BUYING) }
                    val sellingDeferred = async { chatsRepository.listConversationsPage(ChatRole.SELLING) }
                    val buyingPage = buyingDeferred.await()
                    val sellingPage = sellingDeferred.await()
                    nextCursorBuying = buyingPage.nextCursor
                    nextCursorSelling = sellingPage.nextCursor
                    _hasMore.value = buyingPage.nextCursor != null || sellingPage.nextCursor != null

                    val merged = (buyingPage.conversations.map { it to ChatRole.BUYING } +
                                  sellingPage.conversations.map { it to ChatRole.SELLING })
                        .sortedByDescending { (conv, _) -> conv.createdAt }

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
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value) return
        _isLoadingMore.value = true
        try {
            runCatching {
                coroutineScope {
                    val buyingDeferred = nextCursorBuying?.let { cursor ->
                        async { chatsRepository.listConversationsPage(ChatRole.BUYING, cursor = cursor) }
                    }
                    val sellingDeferred = nextCursorSelling?.let { cursor ->
                        async { chatsRepository.listConversationsPage(ChatRole.SELLING, cursor = cursor) }
                    }
                    val buyingPage = buyingDeferred?.await()
                    val sellingPage = sellingDeferred?.await()

                    if (buyingPage != null) nextCursorBuying = buyingPage.nextCursor
                    if (sellingPage != null) nextCursorSelling = sellingPage.nextCursor
                    _hasMore.value = nextCursorBuying != null || nextCursorSelling != null

                    val newConversations = (
                        (buyingPage?.conversations?.map { it to ChatRole.BUYING } ?: emptyList()) +
                        (sellingPage?.conversations?.map { it to ChatRole.SELLING } ?: emptyList())
                    ).sortedByDescending { (conv, _) -> conv.createdAt }

                    val newSummaries = newConversations.map { (conv, role) ->
                        async { buildSummary(conv, role) }
                    }.awaitAll()

                    _chats.update { it + newSummaries }
                    _unreadCount.update { it + newSummaries.sumOf { s -> s.unreadCount } }
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
            }
        } finally {
            _isLoadingMore.value = false
        }
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
}
