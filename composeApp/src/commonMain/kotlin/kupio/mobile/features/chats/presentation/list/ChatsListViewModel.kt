package kupio.mobile.features.chats.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.features.chats.data.ConversationsStore

class ChatsListViewModel(
    private val store: ConversationsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsListState())
    val state: StateFlow<ChatsListState> = _state.asStateFlow()

    private val effectChannel = Channel<ChatsListEffect>(Channel.BUFFERED)
    val effects: Flow<ChatsListEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { store.chats.collect { chats -> _state.update { it.copy(chats = chats) } } }
        viewModelScope.launch { store.isLoading.collect { loading -> _state.update { it.copy(isLoading = loading) } } }
        viewModelScope.launch { store.isLoadingMore.collect { loading -> _state.update { it.copy(isLoadingMore = loading) } } }
        viewModelScope.launch { store.hasMore.collect { more -> _state.update { it.copy(hasMore = more) } } }
        viewModelScope.launch { store.error.collect { error -> _state.update { it.copy(errorMessage = error) } } }
        viewModelScope.launch { store.unreadCount.collect { count -> _state.update { it.copy(totalUnread = count) } } }
    }

    fun onIntent(intent: ChatsListIntent) {
        when (intent) {
            is ChatsListIntent.SelectFilter -> _state.update { it.copy(filter = intent.filter) }
            is ChatsListIntent.OpenChat -> viewModelScope.launch {
                effectChannel.send(ChatsListEffect.OpenChat(intent.id))
            }
            ChatsListIntent.LoadConversations, ChatsListIntent.RetryLoad -> viewModelScope.launch {
                store.refresh()
            }
            ChatsListIntent.RefreshChats -> viewModelScope.launch {
                _state.update { it.copy(isRefreshing = true) }
                store.refresh()
                _state.update { it.copy(isRefreshing = false) }
            }
            ChatsListIntent.LoadMore -> viewModelScope.launch {
                store.loadMore()
            }
            ChatsListIntent.OpenSearch -> {}
            ChatsListIntent.OpenFilters -> {}
        }
    }
}
