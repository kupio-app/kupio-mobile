package kupio.mobile.features.chats.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioUserAvatar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.chats.domain.model.ChatSummary
import kupio.mobile.features.chats.presentation.components.ChatsFilterChips
import kupio.mobile.features.chats.presentation.components.ListingPrice
import kupio.mobile.features.chats.presentation.components.ListingStrip
import kupio.mobile.features.chats.presentation.components.RoleChip
import kupio.mobile.features.chats.presentation.thread.ChatThreadScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chats_empty_body
import mobile.composeapp.generated.resources.chats_empty_footer
import mobile.composeapp.generated.resources.chats_load_error
import mobile.composeapp.generated.resources.chats_title
import mobile.composeapp.generated.resources.chats_unread_subtitle
import mobile.composeapp.generated.resources.retry
import mobile.composeapp.generated.resources.topbar_search
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class ChatsListScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<ChatsListViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.onIntent(ChatsListIntent.LoadConversations)
        }

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is ChatsListEffect.OpenChat -> rootNavigator.push(ChatThreadScreen(effect.id))
            }
        }
        ChatsListContent(state = state, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsListContent(
    state: ChatsListState,
    onIntent: (ChatsListIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val unreadSubtitle = if (state.totalUnread > 0) {
        stringResource(Res.string.chats_unread_subtitle, state.totalUnread)
    } else null

    KupioScaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.chats_title),
                subtitle = unreadSubtitle,
                trailingContent = {
                    KupioTopBarIconAction(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(Res.string.topbar_search),
                        onClick = { onIntent(ChatsListIntent.OpenSearch) },
                    )
                },
            )
        },
    ) {
        PullToRefreshBox (
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(ChatsListIntent.RefreshChats) },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ChatsFilterChips(
                    selected = state.filter,
                    counts = state.counts,
                    onSelect = { onIntent(ChatsListIntent.SelectFilter(it)) },
                    modifier = Modifier.padding(vertical = spacing.md),
                )

                when {
                    state.isLoading && state.chats.isEmpty() -> KupioLoadingScreen()

                    state.errorMessage != null && state.chats.isEmpty() -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.chats_load_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(spacing.sm))
                        Button(onClick = { onIntent(ChatsListIntent.RetryLoad) }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }

                    state.visibleChats.isEmpty() -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.chats_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = spacing.xl),
                    ) {
                        items(state.visibleChats, key = { it.id }) { chat ->
                            ChatCard(chat = chat, onClick = { onIntent(ChatsListIntent.OpenChat(chat.id)) })
                            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
                        }
                        item(key = "footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.xl),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.chats_empty_footer),
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatCard(
    chat: ChatSummary,
    onClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable(onClick)
            .padding(vertical = spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        KupioUserAvatar(
            initials = chat.participantInitials,
            size = 48.dp,
        )
        Spacer(Modifier.width(spacing.md))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.participantLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(spacing.sm))
                RoleChip(role = chat.role)
                Spacer(Modifier.weight(1f))
                Text(
                    text = chat.lastMessageTimeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }

            ListingStrip(
                listing = chat.listing,
                trailing = { ListingPrice(chat.listing.priceFormatted) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chat.lastMessagePreview ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chat.unreadCount > 0) colors.onSurface else colors.onSurfaceVariant,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(spacing.sm))
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.size(22.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (chat.unreadCount > 9) "9+" else chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
