package kupio.mobile.features.chats.presentation.thread

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.domain.model.MessageItem

data class ChatThreadState(
    val chat: ChatSummary? = null,
    val messages: List<MessageItem> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface ChatThreadIntent : UiAction {
    data class DraftChanged(val text: String) : ChatThreadIntent
    data object SendMessage : ChatThreadIntent
    data object OpenListing : ChatThreadIntent
    data object OpenProfile : ChatThreadIntent
    data object Back : ChatThreadIntent
    data object RetryLoad : ChatThreadIntent
}

sealed interface ChatThreadEffect : UiEffect {
    data class OpenListing(val id: String) : ChatThreadEffect
    data class OpenProfile(val participantId: String, val label: String) : ChatThreadEffect
    data object Back : ChatThreadEffect
}
