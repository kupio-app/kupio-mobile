package kupio.mobile.features.reports.presentation.moderator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.presentation.components.ListingThumbnail
import kupio.mobile.features.reports.domain.model.ReportListItem
import kupio.mobile.features.reports.domain.model.ReportsDashboardStats
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.moderator_reports_desc
import mobile.composeapp.generated.resources.moderator_reports_empty
import mobile.composeapp.generated.resources.moderator_reports_filter_in_queue
import mobile.composeapp.generated.resources.moderator_reports_filter_no_action
import mobile.composeapp.generated.resources.moderator_reports_filter_unseen
import mobile.composeapp.generated.resources.moderator_reports_load_error
import mobile.composeapp.generated.resources.moderator_reports_title
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class ModeratorReportsDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ModeratorReportsDashboardViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val isFirstResume = remember { mutableStateOf(true) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            if (isFirstResume.value) {
                isFirstResume.value = false
            } else {
                viewModel.onIntent(ModeratorReportsDashboardIntent.Refresh)
            }
        }

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                ModeratorReportsDashboardEffect.NavigateBack -> navigator.pop()
                is ModeratorReportsDashboardEffect.NavigateToReportDetail ->
                    navigator.push(ModeratorReportDetailScreen(effect.reportId))
            }
        }

        ModeratorReportsDashboardRoute(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun ModeratorReportsDashboardRoute(
    state: ModeratorReportsDashboardState,
    onIntent: (ModeratorReportsDashboardIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.moderator_reports_title),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(ModeratorReportsDashboardIntent.BackClicked) },
                    )
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(ModeratorReportsDashboardIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ReportsFilterStatsRow(
                    selectedFilter = state.filter,
                    stats = state.stats,
                    onFilterSelected = { onIntent(ModeratorReportsDashboardIntent.FilterSelected(it)) },
                )

                when {
                    state.isLoading -> KupioLoadingScreen()
                    state.errorMessage != null && state.reports.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            KupioErrorRetryRow(
                                message = stringResource(Res.string.moderator_reports_load_error),
                                onRetry = { onIntent(ModeratorReportsDashboardIntent.RetryLoad) },
                            )
                        }
                    }
                    state.reports.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.moderator_reports_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = spacing.lg,
                                vertical = spacing.sm,
                            ),
                        ) {
                            itemsIndexed(state.reports, key = { _, r -> r.id }) { index, report ->
                                ReportCard(
                                    report = report,
                                    onClick = {
                                        onIntent(ModeratorReportsDashboardIntent.ReportClicked(report.id))
                                    },
                                )
                                // trigger load more when we reach the last few items
                                if (index == state.reports.size - 3 && state.hasMore) {
                                    onIntent(ModeratorReportsDashboardIntent.LoadMore)
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
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(spacing.xl)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsFilterStatsRow(
    selectedFilter: ReportsDashboardFilter,
    stats: ReportsDashboardStats?,
    onFilterSelected: (ReportsDashboardFilter) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val filters = listOf(
        Triple(ReportsDashboardFilter.IN_QUEUE, stringResource(Res.string.moderator_reports_filter_in_queue), stats?.inQueue ?: 0),
        Triple(ReportsDashboardFilter.UNSEEN, stringResource(Res.string.moderator_reports_filter_unseen), stats?.unseen ?: 0),
        Triple(ReportsDashboardFilter.NO_ACTION, stringResource(Res.string.moderator_reports_filter_no_action), stats?.noAction ?: 0),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        filters.forEach { (filter, label, count) ->
            ReportFilterStatCard(
                modifier = Modifier.weight(1f),
                label = label,
                count = count,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun ReportFilterStatCard(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        modifier = modifier.bouncingClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = if (selected) {
            BorderStroke(KupioThemeDefaults.borderWidths.regular, colors.primary)
        } else {
            KupioThemeDefaults.defaultBorder
        },
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) colors.primary else colors.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.primary.copy(alpha = 0.7f) else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: ReportListItem,
    onClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val descShortId = report.listingId.takeLast(8)
    val descText = stringResource(
        Res.string.moderator_reports_desc,
        report.createdAt.take(10),
        report.sellerUsername,
        descShortId,
    )

    KupioCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ListingThumbnail(
                imageUrl = report.listingImageUrl,
                contentDescription = report.listingTitle,
                modifier = Modifier.size(64.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = report.reasonTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
