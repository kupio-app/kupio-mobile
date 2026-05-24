package kupio.mobile.features.payments.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.number
import kupio.mobile.core.datetime.monthName
import kupio.mobile.core.datetime.toLocalDate
import kupio.mobile.core.datetime.toTimeLabel
import kupio.mobile.core.designsystem.KupioFilterChip
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.payments.domain.model.BalanceTransaction
import kupio.mobile.features.payments.domain.model.TransactionType
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.payments_empty
import mobile.composeapp.generated.resources.payments_filter_all
import mobile.composeapp.generated.resources.payments_filter_spending
import mobile.composeapp.generated.resources.payments_filter_topups
import mobile.composeapp.generated.resources.payments_status_completed
import mobile.composeapp.generated.resources.payments_status_refunded
import mobile.composeapp.generated.resources.payments_subtitle
import mobile.composeapp.generated.resources.payments_title
import mobile.composeapp.generated.resources.payments_txn_count
import mobile.composeapp.generated.resources.payments_type_payment
import mobile.composeapp.generated.resources.payments_type_refund
import mobile.composeapp.generated.resources.payments_type_topup
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PositiveColor = Color(0xFF4A7C59)

class PaymentsHistoryScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<PaymentsHistoryViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                PaymentsHistoryEffect.NavigateBack -> navigator.pop()
            }
        }

        PaymentsHistoryContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PaymentsHistoryContent(state: PaymentsHistoryState, onIntent: (PaymentsHistoryIntent) -> Unit) {
    val spacing = KupioThemeDefaults.spacing

    val filteredTransactions = remember(state.transactions, state.filter) {
        when (state.filter) {
            PaymentsFilter.ALL -> state.transactions
            PaymentsFilter.TOP_UPS -> state.transactions.filter { it.type == TransactionType.TOP_UP }
            PaymentsFilter.SPENDING -> state.transactions.filter {
                it.type == TransactionType.DEBIT || it.type == TransactionType.REFUND
            }
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { transaction ->
                val date = transaction.createdAt.toLocalDate()
                if (date != null) Pair(date.year, date.month.number) else Pair(0, 0)
            }
            .entries
            .sortedByDescending { (key, _) -> key.first * 12 + key.second }
    }

    Scaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.payments_title),
                subtitle = stringResource(Res.string.payments_subtitle),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(PaymentsHistoryIntent.BackClicked) },
                    )
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(PaymentsHistoryIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (state.isLoading) {
                KupioLoadingScreen()
            } else {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        lastVisible >= total - 3 && !state.isLoadingMore && state.hasMore && total > 0
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) onIntent(PaymentsHistoryIntent.LoadMore)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = spacing.md,
                        end = spacing.md,
                        top = spacing.md,
                        bottom = spacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    item {
                        FilterRow(
                            selectedFilter = state.filter,
                            onFilterSelected = { onIntent(PaymentsHistoryIntent.FilterSelected(it)) },
                        )
                    }

                    if (groupedTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.xl),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.payments_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        groupedTransactions.forEach { (monthYear, transactions) ->
                            item(key = "${monthYear.first}-${monthYear.second}-header") {
                                MonthGroupHeader(
                                    year = monthYear.first,
                                    month = monthYear.second,
                                    count = transactions.size,
                                )
                            }
                            item(key = "${monthYear.first}-${monthYear.second}-card") {
                                MonthGroupCard(transactions = transactions)
                            }
                        }
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
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
private fun FilterRow(selectedFilter: PaymentsFilter, onFilterSelected: (PaymentsFilter) -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        item {
            KupioFilterChip(
                label = stringResource(Res.string.payments_filter_all),
                selected = selectedFilter == PaymentsFilter.ALL,
                onClick = { onFilterSelected(PaymentsFilter.ALL) },
            )
        }
        item {
            KupioFilterChip(
                label = stringResource(Res.string.payments_filter_topups),
                selected = selectedFilter == PaymentsFilter.TOP_UPS,
                onClick = { onFilterSelected(PaymentsFilter.TOP_UPS) },
            )
        }
        item {
            KupioFilterChip(
                label = stringResource(Res.string.payments_filter_spending),
                selected = selectedFilter == PaymentsFilter.SPENDING,
                onClick = { onFilterSelected(PaymentsFilter.SPENDING) },
            )
        }
    }
}

@Composable
private fun MonthGroupHeader(year: Int, month: Int, count: Int) {
    val spacing = KupioThemeDefaults.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${monthName(month).uppercase()} $year",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.payments_txn_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthGroupCard(transactions: List<BalanceTransaction>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            transactions.forEachIndexed { index, transaction ->
                TransactionRow(transaction = transaction)
                if (index < transactions.lastIndex) {
                    HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: BalanceTransaction) {
    val spacing = KupioThemeDefaults.spacing
    val typeLabel = when (transaction.type) {
        TransactionType.TOP_UP -> stringResource(Res.string.payments_type_topup)
        TransactionType.DEBIT -> stringResource(Res.string.payments_type_payment)
        TransactionType.REFUND -> stringResource(Res.string.payments_type_refund)
    }
    val statusLabel = when (transaction.type) {
        TransactionType.REFUND -> stringResource(Res.string.payments_status_refunded)
        else -> stringResource(Res.string.payments_status_completed)
    }
    val isPositive = transaction.type == TransactionType.TOP_UP || transaction.type == TransactionType.REFUND
    val amountText = formatTransactionAmount(transaction.amountCents, transaction.type)
    val amountColor = if (isPositive) PositiveColor else MaterialTheme.colorScheme.onSurface

    val date = transaction.createdAt.toLocalDate()
    val timeLabel = transaction.createdAt.toTimeLabel()
    val dateLabel = if (date != null) {
        "${date.day} ${monthName(date.month.number).take(3)} · $timeLabel"
    } else {
        timeLabel
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        TransactionIcon(type = transaction.type)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$dateLabel · $statusLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor,
            )
            Text(
                text = "#${transaction.id.take(8)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionIcon(type: TransactionType) {
    val (icon, bgColor, tintColor) = when (type) {
        TransactionType.TOP_UP -> Triple(
            Icons.Outlined.AccountBalanceWallet,
            PositiveColor.copy(alpha = 0.12f),
            PositiveColor,
        )
        TransactionType.DEBIT -> Triple(
            Icons.Outlined.Bolt,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
        )
        TransactionType.REFUND -> Triple(
            Icons.AutoMirrored.Outlined.Undo,
            PositiveColor.copy(alpha = 0.12f),
            PositiveColor,
        )
    }
    TransactionIconBox(icon = icon, bgColor = bgColor, tintColor = tintColor)
}

@Composable
private fun TransactionIconBox(icon: ImageVector, bgColor: Color, tintColor: Color) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = KupioShapes.Medium,
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatTransactionAmount(amountCents: Int, type: TransactionType): String {
    val whole = amountCents / 100
    val fraction = (amountCents % 100).toString().padStart(2, '0')
    return when (type) {
        TransactionType.TOP_UP, TransactionType.REFUND -> "+$whole.$fraction €"
        TransactionType.DEBIT -> "-$whole.$fraction €"
    }
}
