package kupio.mobile.features.chats.presentation.list

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.chats.domain.model.ChatRole
import kupio.mobile.features.chats.domain.model.ChatSummary

enum class ChatsFilter { ALL, BUYING, SELLING }

data class ChatsListState(
    val chats: List<ChatSummary> = emptyList(),
    val filter: ChatsFilter = ChatsFilter.ALL,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val totalUnread: Int = 0,
) : UiState {

    val visibleChats: List<ChatSummary> get() = when (filter) {
        ChatsFilter.ALL -> chats
        ChatsFilter.BUYING -> chats.filter { it.role == ChatRole.BUYING }
        ChatsFilter.SELLING -> chats.filter { it.role == ChatRole.SELLING }
    }

    val counts: Map<ChatsFilter, Int> get() = mapOf(
        ChatsFilter.ALL to chats.size,
        ChatsFilter.BUYING to chats.count { it.role == ChatRole.BUYING },
        ChatsFilter.SELLING to chats.count { it.role == ChatRole.SELLING },
    )
}

sealed interface ChatsListIntent : UiAction {
    data class SelectFilter(val filter: ChatsFilter) : ChatsListIntent
    data class OpenChat(val id: String) : ChatsListIntent
    data object RefreshChats : ChatsListIntent
    data object OpenSearch : ChatsListIntent
    data object OpenFilters : ChatsListIntent
    data object RetryLoad : ChatsListIntent
}

sealed interface ChatsListEffect : UiEffect {
    data class OpenChat(val id: String) : ChatsListEffect
}
