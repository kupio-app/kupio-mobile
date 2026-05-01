package kupio.mobile.features.reports.presentation.moderator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioUserAvatar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.presentation.components.ListingThumbnail
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import kupio.mobile.features.reports.domain.model.ReportDetail
import kupio.mobile.features.reports.domain.model.formatPrice
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.moderator_report_detail_comment_label
import mobile.composeapp.generated.resources.moderator_report_detail_comment_placeholder
import mobile.composeapp.generated.resources.moderator_report_detail_decision_ban_user
import mobile.composeapp.generated.resources.moderator_report_detail_decision_decline
import mobile.composeapp.generated.resources.moderator_report_detail_decision_label
import mobile.composeapp.generated.resources.moderator_report_detail_decision_remove_listing
import mobile.composeapp.generated.resources.moderator_report_detail_reporter_note_label
import mobile.composeapp.generated.resources.moderator_report_detail_seller_label
import mobile.composeapp.generated.resources.moderator_report_detail_submit
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class ModeratorReportDetailScreen(val reportId: Int) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ModeratorReportDetailViewModel>(
            key = "report-detail-$reportId",
        ) { parametersOf(reportId) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                ModeratorReportDetailEffect.NavigateBack -> navigator.pop()
                is ModeratorReportDetailEffect.ShowSuccess -> navigator.pop()
            }
        }

        ModeratorReportDetailContent(
            state = state,
            onIntent = viewModel::onIntent,
            onViewListing = { listingId -> navigator.push(ListingDetailScreen(listingId)) },
        )
    }
}

@Composable
private fun ModeratorReportDetailContent(
    state: ModeratorReportDetailState,
    onIntent: (ModeratorReportDetailIntent) -> Unit,
    onViewListing: (String) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KupioTopNavbar(
                title = "Report #${ state.report?.id ?: "" }",
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(ModeratorReportDetailIntent.BackClicked) },
                    )
                },
            )
        },
        bottomBar = {
            if (!state.isLoading && state.errorMessage == null) {
                SubmitBar(
                    isSubmitting = state.isSubmitting,
                    enabled = state.selectedDecision != null && !state.isSubmitting,
                    onSubmit = { onIntent(ModeratorReportDetailIntent.SubmitDecision) },
                )
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> KupioLoadingScreen()
            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                KupioErrorRetryRow(
                    message = state.errorMessage,
                    onRetry = { onIntent(ModeratorReportDetailIntent.Retry) },
                )
            }
            state.report != null -> ReportDetailBody(
                report = state.report,
                state = state,
                paddingValues = paddingValues,
                onIntent = onIntent,
                onViewListing = { onViewListing(state.report.listing.id) },
            )
        }
    }
}

@Composable
private fun ReportDetailBody(
    report: ReportDetail,
    state: ModeratorReportDetailState,
    paddingValues: PaddingValues,
    onIntent: (ModeratorReportDetailIntent) -> Unit,
    onViewListing: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(bottom = spacing.xl),
    ) {
        item {
            ListingPreviewCard(
                report = report,
                onViewListing = onViewListing,
            )
        }
        if (!report.additionalInfo.isNullOrBlank()) {
            item {
                ReporterNoteSection(
                    label = stringResource(Res.string.moderator_report_detail_reporter_note_label),
                    note = report.additionalInfo,
                )
            }
        }
        item {
            DecisionSection(
                selected = state.selectedDecision,
                onDecisionSelected = { onIntent(ModeratorReportDetailIntent.DecisionSelected(it)) },
            )
        }
        item {
            CommentSection(
                comment = state.moderatorComment,
                onCommentChanged = { onIntent(ModeratorReportDetailIntent.CommentChanged(it)) },
            )
        }
        if (state.submitError != null) {
            item {
                Text(
                    text = state.submitError,
                    modifier = Modifier.padding(horizontal = spacing.lg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { Spacer(Modifier.height(spacing.md)) }
    }
}

// 1. Listing preview card — thumbnail + info + View button
@Composable
private fun ListingPreviewCard(
    report: ReportDetail,
    onViewListing: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val listing = report.listing
    val sellerName = report.sellerDisplayName.takeIf { it.isNotBlank() } ?: report.sellerUsername
    val sellerInitials = sellerName.take(2).uppercase()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                ListingThumbnail(
                    imageUrl = listing.primaryImageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.size(80.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Surface(
                        shape = KupioShapes.Small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = report.reasonTitle.uppercase(),
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listing.formatPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        KupioUserAvatar(initials = sellerInitials, size = 20.dp)
                        Text(
                            text = sellerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(
                            Res.string.moderator_report_detail_seller_label,
                            report.createdAt.take(10),
                            listing.id.takeLast(8),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Black "view" icon button in top-right corner
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncingClickable(onClick = onViewListing),
                    shape = KupioShapes.Small,
                    color = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}

// 2. Reporter note with white/surface background
@Composable
private fun ReporterNoteSection(label: String, note: String) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionLabel(label)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = KupioShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = KupioThemeDefaults.defaultBorder,
        ) {
            Text(
                text = note,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 3. Decision section — 2-column top row + full-width bottom row
@Composable
private fun DecisionSection(
    selected: ReportDecision?,
    onDecisionSelected: (ReportDecision) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val declineLabel = stringResource(Res.string.moderator_report_detail_decision_decline)
    val removeLabel = stringResource(Res.string.moderator_report_detail_decision_remove_listing)
    val banLabel = stringResource(Res.string.moderator_report_detail_decision_ban_user)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionLabel(stringResource(Res.string.moderator_report_detail_decision_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            DecisionOptionCard(
                label = declineLabel,
                icon = Icons.Outlined.Check,
                selected = selected == ReportDecision.DECLINE,
                onClick = { onDecisionSelected(ReportDecision.DECLINE) },
                modifier = Modifier.weight(1f),
            )
            DecisionOptionCard(
                label = banLabel,
                icon = Icons.Outlined.Block,
                selected = selected == ReportDecision.BAN_USER,
                onClick = { onDecisionSelected(ReportDecision.BAN_USER) },
                modifier = Modifier.weight(1f),
            )
        }
        DecisionOptionCard(
            label = removeLabel,
            icon = Icons.Outlined.Delete,
            selected = selected == ReportDecision.REMOVE_LISTING,
            onClick = { onDecisionSelected(ReportDecision.REMOVE_LISTING) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DecisionOptionCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.bouncingClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = if (selected) colors.onSurface else colors.surface,
        border = if (selected) null else KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) colors.surface else colors.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.surface else colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// 4. Comment section — no duplicate label: SectionLabel removed, KupioTextField label shows it
@Composable
private fun CommentSection(
    comment: String,
    onCommentChanged: (String) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        KupioTextField(
            value = comment,
            onValueChange = onCommentChanged,
            label = stringResource(Res.string.moderator_report_detail_comment_label),
            placeholder = stringResource(Res.string.moderator_report_detail_comment_placeholder),
            singleLine = false,
            minLines = 3,
        )
    }
}

// 5. Submit bar — styled like publish listing button
@Composable
private fun SubmitBar(
    isSubmitting: Boolean,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.background,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        ) {
            Button(
                onClick = onSubmit,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = KupioShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onSurface,
                    contentColor = colors.surface,
                    disabledContainerColor = colors.outline.copy(alpha = 0.24f),
                    disabledContentColor = colors.surface.copy(alpha = 0.72f),
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.surface,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.moderator_report_detail_submit),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
