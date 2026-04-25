package kupio.mobile.features.chats.presentation.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.chats.domain.repository.ChatsRepository

class ChatThreadViewModel(
    private val conversationId: String,
    private val repo: ChatsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadState())
    val state: StateFlow<ChatThreadState> = _state.asStateFlow()

    private val effectChannel = Channel<ChatThreadEffect>(Channel.BUFFERED)
    val effects: Flow<ChatThreadEffect> = effectChannel.receiveAsFlow()

    init {
        observeConversation()
        loadMessages()
    }

    fun onIntent(intent: ChatThreadIntent) {
        when (intent) {
            is ChatThreadIntent.DraftChanged -> _state.update { it.copy(draft = intent.text) }
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

    private fun observeConversation() {
        viewModelScope.launch {
            repo.observeConversation(conversationId).collect { chat ->
                _state.update { it.copy(chat = chat) }
            }
        }
    }

    private fun loadMessages() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repo.loadMessages(conversationId) }
                .onSuccess { messages ->
                    _state.update { it.copy(messages = messages, isLoading = false) }
                    repo.markSeen(conversationId)
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isLoading = false, errorMessage = t.message) }
                }
        }
    }

    private fun sendMessage() {
        val text = _state.value.draft.trim()
        if (text.isBlank()) return
        _state.update { it.copy(draft = "", isSending = true) }
        viewModelScope.launch {
            runCatching { repo.sendMessage(conversationId, text) }
                .onSuccess { newMsg ->
                    _state.update { it.copy(messages = it.messages + newMsg, isSending = false) }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(isSending = false, draft = text) }
                }
        }
    }
}
