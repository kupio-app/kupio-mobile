package kupio.mobile.features.chats.presentation.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.borderBottom
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.presentation.components.ListingPrice
import kupio.mobile.features.chats.presentation.components.ListingStrip
import kupio.mobile.features.chats.presentation.components.MessageBubble
import kupio.mobile.features.chats.presentation.components.MessageComposer
import kupio.mobile.features.chats.presentation.components.UserAvatar
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import kupio.mobile.features.userprofile.presentation.UserPublicProfileScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chat_active_unknown
import mobile.composeapp.generated.resources.chat_day_today_private
import mobile.composeapp.generated.resources.chat_thread_discussing
import mobile.composeapp.generated.resources.chat_thread_view
import mobile.composeapp.generated.resources.retry
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class ChatThreadScreen(private val conversationId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ChatThreadViewModel> { parametersOf(conversationId) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is ChatThreadEffect.Back -> navigator.pop()
                is ChatThreadEffect.OpenProfile -> navigator.push(
                    UserPublicProfileScreen(effect.participantId, effect.label)
                )
                is ChatThreadEffect.OpenListing -> navigator.push(ListingDetailScreen(effect.id))
            }
        }
        ChatThreadContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun ChatThreadContent(
    state: ChatThreadState,
    onIntent: (ChatThreadIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            ChatThreadTopBar(
                chat = state.chat,
                onBack = { onIntent(ChatThreadIntent.Back) },
                onProfile = { onIntent(ChatThreadIntent.OpenProfile) },
            )
        },
        bottomBar = {
            MessageComposer(
                draft = state.draft,
                onDraftChange = { onIntent(ChatThreadIntent.DraftChanged(it)) },
                onSend = { onIntent(ChatThreadIntent.SendMessage) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            state.chat?.listing?.let { listing ->
                ListingStrip(
                    listing = listing,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(Res.string.chat_thread_discussing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                ListingPrice(listing.priceFormatted)
                            }
                            Spacer(Modifier.width(spacing.xs))
                            Button(
                                onClick = { onIntent(ChatThreadIntent.OpenListing) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface,
                                    contentColor = MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = spacing.md, vertical = 6.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.chat_thread_view),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    },
                )
            }

            when {
                state.isLoading && state.messages.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.errorMessage != null && state.messages.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onIntent(ChatThreadIntent.RetryLoad) }) {
                        Text(stringResource(Res.string.retry))
                    }
                }

                else -> MessagesColumn(
                    messages = state.messages,
                    listState = listState,
                )
            }
        }
    }
}

@Composable
private fun ChatThreadTopBar(
    chat: ChatSummary?,
    onBack: () -> Unit,
    onProfile: () -> Unit,
) {
    KupioTopNavbar(
        title = chat?.participantLabel ?: "",
        subtitle = stringResource(Res.string.chat_active_unknown),
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                KupioTopBarBackAction(
                    contentDescription = stringResource(Res.string.topbar_back),
                    onClick = onBack,
                )
                if (chat != null) {
                    UserAvatar(initials = chat.participantInitials, size = 36.dp)
                }
            }
        },
        trailingContent = {
            KupioTopBarIconAction(
                imageVector = Icons.Default.PersonOutline,
                contentDescription = stringResource(Res.string.chat_active_unknown),
                onClick = onProfile,
            )
        },
    )
}

@Composable
private fun MessagesColumn(
    messages: List<MessageItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val spacing = KupioThemeDefaults.spacing

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "day_label") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(Res.string.chat_day_today_private),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
        items(messages, key = { it.id }) { message ->
            if (!message.isDeleted) {
                MessageBubble(message = message)
            }
        }
    }
}
