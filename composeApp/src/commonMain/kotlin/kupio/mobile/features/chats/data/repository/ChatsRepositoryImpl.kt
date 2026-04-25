package kupio.mobile.features.chats.data.repository

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
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
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.preferences.PreferencesRepository
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.data.remote.ChatApi
import kupio.mobile.features.chats.data.remote.ConversationResponseDto
import kupio.mobile.features.chats.data.remote.ConversationRoleDto
import kupio.mobile.features.chats.data.remote.MessageResponseDto
import kupio.mobile.features.chats.data.remote.UserApi
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.domain.model.ListingSummary
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.MessageSender
import kupio.mobile.features.chats.domain.repository.ChatsRepository
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.domain.repository.ListingsRepository

class ChatsRepositoryImpl(
    private val chatApi: ChatApi,
    private val listingsRepository: ListingsRepository,
    private val userApi: UserApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val preferences: PreferencesRepository,
    private val sessionManager: AuthSessionManager,
) : ChatsRepository {

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _unreadCount = MutableStateFlow(0)

    override fun observeChats(): StateFlow<List<ChatSummary>> = _chats.asStateFlow()
    override fun observeLoading(): StateFlow<Boolean> = _loading.asStateFlow()
    override fun observeError(): StateFlow<String?> = _error.asStateFlow()
    override fun observeUnreadCount(): StateFlow<Int> = _unreadCount.asStateFlow()

    override fun observeConversation(id: String): Flow<ChatSummary?> =
        _chats.map { list -> list.find { it.id == id } }

    override suspend fun refresh() {
        _loading.value = true
        _error.value = null
        runCatching {
            coroutineScope {
                val buyerDeferred = async {
                    authenticatedApiClient.request { authorize ->
                        chatApi.listConversations(authorize, ConversationRoleDto.BUYER)
                    }.conversations
                }
                val sellerDeferred = async {
                    authenticatedApiClient.request { authorize ->
                        chatApi.listConversations(authorize, ConversationRoleDto.SELLER)
                    }.conversations
                }
                val buyerConvs = buyerDeferred.await().map { it to ChatRole.BUYING }
                val sellerConvs = sellerDeferred.await().map { it to ChatRole.SELLING }
                val merged = (buyerConvs + sellerConvs)
                    .sortedByDescending { (conv, _) -> conv.createdAt }

                val currentUserId = (sessionManager.sessionState.value as? SessionState.SignedIn)
                    ?.user?.id.orEmpty()

                val summaries = merged.map { (conv, role) ->
                    async { buildSummary(conv, role, currentUserId) }
                }.awaitAll()

                _chats.value = summaries
                _unreadCount.value = summaries.sumOf { it.unreadCount }
            }
        }.onFailure { t ->
            if (t is CancellationException) throw t
            _error.value = t.message ?: "Unknown error"
        }
        _loading.value = false
    }

    override suspend fun refreshUnreadCount() {
        runCatching {
            authenticatedApiClient.request { authorize ->
                chatApi.getUnreadCount(authorize)
            }.unreadCount
        }.onSuccess { count ->
            _unreadCount.value = count
        }
    }

    override suspend fun loadMessages(conversationId: String): List<MessageItem> {
        val currentUserId = (sessionManager.sessionState.value as? SessionState.SignedIn)
            ?.user?.id.orEmpty()
        return authenticatedApiClient.request { authorize ->
            chatApi.listMessages(authorize, conversationId)
        }.messages.reversed().map { it.toItem(currentUserId) }
    }

    override suspend fun sendMessage(conversationId: String, body: String): MessageItem {
        val currentUserId = (sessionManager.sessionState.value as? SessionState.SignedIn)
            ?.user?.id.orEmpty()
        val dto = authenticatedApiClient.request { authorize ->
            chatApi.sendMessage(authorize, conversationId, body)
        }
        _chats.update { list ->
            list.map { chat ->
                if (chat.id == conversationId) chat.copy(
                    lastMessagePreview = body,
                    lastMessageTimeLabel = dto.createdAt.toTimeLabel(),
                ) else chat
            }
        }
        return dto.toItem(currentUserId)
    }

    override suspend fun markSeen(conversationId: String) {
        preferences.markChatSeen(conversationId, currentEpochMillis())
        _chats.update { list ->
            val prev = list.find { it.id == conversationId }?.unreadCount ?: 0
            _unreadCount.update { (it - prev).coerceAtLeast(0) }
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
    }

    private suspend fun buildSummary(
        conv: ConversationResponseDto,
        role: ChatRole,
        currentUserId: String,
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

private fun Listing.toSummary() = ListingSummary(
    id = id,
    title = title,
    priceFormatted = formatPrice(),
    placeholderSeed = id.hashCode(),
)

private fun MessageResponseDto.toItem(currentUserId: String) = MessageItem(
    id = id,
    sender = if (senderId == currentUserId) MessageSender.ME else MessageSender.THEM,
    text = content ?: "",
    timeLabel = createdAt.toTimeLabel(),
    isDeleted = isDeleted,
)

private fun String.toTimeLabel(): String {
    return try {
        val tIndex = indexOf('T')
        if (tIndex < 0) return this
        val timePart = substring(tIndex + 1)
        val colonIndex = timePart.indexOf(':')
        if (colonIndex < 0) return this
        val secondColonIndex = timePart.indexOf(':', colonIndex + 1)
        if (secondColonIndex < 0) return this
        timePart.substring(0, secondColonIndex)
    } catch (_: Exception) {
        this
    }
}

@OptIn(ExperimentalTime::class)
private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
