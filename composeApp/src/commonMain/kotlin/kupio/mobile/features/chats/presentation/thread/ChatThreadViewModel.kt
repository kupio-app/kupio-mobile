package kupio.mobile.features.chats.presentation.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.datetime.nowEpochMillis
import kupio.mobile.features.chats.data.ConversationsStore
import kupio.mobile.features.chats.domain.model.WsMessageEvent
import kupio.mobile.features.chats.domain.repository.MessagesRepository
import kupio.mobile.core.analytics.AnalyticsService
import kotlin.time.Duration.Companion.milliseconds

private const val TYPING_THROTTLE_MS = 2_000L
private const val TYPING_HIDE_MS = 3_000L

class ChatThreadViewModel(
    private val conversationId: String,
    private val store: ConversationsStore,
    private val messagesRepo: MessagesRepository,
    private val analytics: AnalyticsService,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadState())
    val state: StateFlow<ChatThreadState> = _state.asStateFlow()

    private val effectChannel = Channel<ChatThreadEffect>(Channel.BUFFERED)
    val effects: Flow<ChatThreadEffect> = effectChannel.receiveAsFlow()

    private var lastTypingSentAtMs = 0L
    private var typingHideJob: Job? = null

    init {
        observeConversation()
        loadMessages()
        observeMessages()
    }

    fun onIntent(intent: ChatThreadIntent) {
        when (intent) {
            is ChatThreadIntent.DraftChanged -> {
                _state.update { it.copy(draft = intent.text) }
                if (intent.text.isNotEmpty()) throttledSendTyping()
            }
            ChatThreadIntent.SendMessage -> sendMessage()
            ChatThreadIntent.OpenListing -> {
                val listingId = _state.value.chat?.listing?.id ?: return
                viewModelScope.launch { effectChannel.send(ChatThreadEffect.OpenListing(listingId)) }
            }
            ChatThreadIntent.OpenProfile -> {
                val chat = _state.value.chat ?: return
                viewModelScope.launch {
                    effectChannel.send(ChatThreadEffect.OpenProfile(chat.participantId, chat.participantLabel))
                }
            }
            ChatThreadIntent.Back -> viewModelScope.launch { effectChannel.send(ChatThreadEffect.Back) }
            ChatThreadIntent.RetryLoad -> loadMessages()
        }
    }

    private fun throttledSendTyping() {
        val now = nowEpochMillis()
        if (now - lastTypingSentAtMs < TYPING_THROTTLE_MS) return
        lastTypingSentAtMs = now
        viewModelScope.launch {
            runCatching { messagesRepo.sendTyping(conversationId) }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            messagesRepo.observeMessages(conversationId).collect { event ->
                when (event) {
                    is WsMessageEvent.Received -> {
                        if (_state.value.messages.none { it.id == event.message.id }) {
                            _state.update { it.copy(messages = it.messages + event.message) }
                        }
                        store.updatePreview(conversationId, event.message.text, event.message.timeLabel)
                    }
                    is WsMessageEvent.Deleted -> {
                        _state.update { state ->
                            state.copy(messages = state.messages.map { msg ->
                                if (msg.id == event.messageId) msg.copy(isDeleted = true, text = "") else msg
                            })
                        }
                    }
                    WsMessageEvent.TypingStarted -> {
                        _state.update { it.copy(isParticipantTyping = true) }
                        typingHideJob?.cancel()
                        typingHideJob = viewModelScope.launch {
                            delay(TYPING_HIDE_MS.milliseconds)
                            _state.update { it.copy(isParticipantTyping = false) }
                        }
                    }
                }
            }
        }
    }

    private fun observeConversation() {
        viewModelScope.launch {
            store.observeConversation(conversationId).collect { chat ->
                _state.update { it.copy(chat = chat) }
            }
        }
    }

    private fun loadMessages() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { messagesRepo.loadMessages(conversationId) }
                .onSuccess { messages ->
                    _state.update { it.copy(messages = messages, isLoading = false) }
                    store.markSeen(conversationId)
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message) }
                    analytics.recordException(t, mapOf("screen" to "chat_thread", "action" to "load"))
                }
        }
    }

    private fun sendMessage() {
        val text = _state.value.draft.trim()
        if (text.isBlank()) return
        _state.update { it.copy(draft = "", isSending = true) }
        viewModelScope.launch {
            runCatching { messagesRepo.sendMessage(conversationId, text) }
                .onSuccess { newMsg ->
                    _state.update { state ->
                        val messages = if (state.messages.none { it.id == newMsg.id }) {
                            state.messages + newMsg
                        } else {
                            state.messages
                        }
                        state.copy(messages = messages, isSending = false)
                    }
                    store.updatePreview(conversationId, newMsg.text, newMsg.timeLabel)
                    analytics.logEvent("send_message", mapOf("conversation_id" to conversationId))
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isSending = false, draft = text) }
                    analytics.recordException(t, mapOf("screen" to "chat_thread"))
                }
        }
    }
}
