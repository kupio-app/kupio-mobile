package kupio.mobile.features.reports.presentation.create
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.presentation.components.ListingThumbnail
import kupio.mobile.features.reports.domain.model.ReportReason
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.report_listing_additional_info_label
import mobile.composeapp.generated.resources.report_listing_additional_info_placeholder
import mobile.composeapp.generated.resources.report_listing_reasons_label
import mobile.composeapp.generated.resources.report_listing_submit
import mobile.composeapp.generated.resources.report_listing_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val AdditionalInfoMaxLength = 2000

data class CreateReportScreen(
    val listingId: String,
    val listingTitle: String,
    val listingImageUrl: String = "",
    val listingPriceFormatted: String = "",
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<CreateReportViewModel>(
            key = "create-report-$listingId",
        ) { parametersOf(listingId, listingTitle, listingImageUrl, listingPriceFormatted) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                CreateReportEffect.NavigateBack -> navigator.pop()
            }
        }

        CreateReportContent(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun CreateReportContent(
    state: CreateReportState,
    onIntent: (CreateReportIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.report_listing_title),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.back),
                        onClick = { onIntent(CreateReportIntent.Back) },
                    )
                },
            )
        },
        bottomBar = {
            ReportSubmitBar(
                canSubmit = state.canSubmit,
                isSubmitting = state.isSubmitting,
                onSubmit = { onIntent(CreateReportIntent.Submit) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            state.isLoadingReasons -> KupioLoadingScreen(
                modifier = Modifier.padding(paddingValues),
            )

            state.reasonsError != null -> {
                KupioErrorRetryRow(
                    message = state.reasonsError,
                    onRetry = { onIntent(CreateReportIntent.RetryReasons) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(spacing.md),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // Compact listing strip
                    item(key = "listing_header") {
                        ListingHeroCard(
                            title = state.listingTitle,
                            imageUrl = state.listingImageUrl,
                            priceFormatted = state.listingPriceFormatted,
                        )
                    }

                    // Reasons section label
                    item(key = "reasons_label") {
                        Text(
                            text = stringResource(Res.string.report_listing_reasons_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = spacing.md,
                                end = spacing.md,
                                top = spacing.md,
                                bottom = spacing.xs,
                            ),
                        )
                    }

                    // Reason rows
                    items(state.reasons, key = { it.id }) { reason ->
                        ReasonRow(
                            reason = reason,
                            selected = reason.id == state.selectedReasonId,
                            onClick = { onIntent(CreateReportIntent.SelectReason(reason.id)) },
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = 4.dp),
                        )
                    }

                    // Additional info field
                    item(key = "additional_info") {
                        AdditionalInfoSection(
                            value = state.additionalInfo,
                            onValueChange = { onIntent(CreateReportIntent.AdditionalInfoChanged(it)) },
                            modifier = Modifier.padding(
                                horizontal = spacing.md,
                                vertical = spacing.sm,
                            ),
                        )
                    }

                    // Submit error
                    state.submitError?.let { error ->
                        item(key = "submit_error") {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = spacing.md),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingHeroCard(
    title: String,
    imageUrl: String,
    priceFormatted: String,
) {
    val spacing = KupioThemeDefaults.spacing
    KupioCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListingThumbnail(
                imageUrl = imageUrl.takeIf { it.isNotBlank() },
                contentDescription = title,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            if (priceFormatted.isNotBlank()) {
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = priceFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ReasonRow(
    reason: ReportReason,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bouncingClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = KupioThemeDefaults.borderWidths.regular,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            KupioThemeDefaults.defaultBorder
        },
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = reason.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!reason.description.isNullOrBlank()) {
                    Text(
                        text = reason.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoSection(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        KupioTextField(
            value = value,
            onValueChange = { if (it.length <= AdditionalInfoMaxLength) onValueChange(it) },
            label = stringResource(Res.string.report_listing_additional_info_label),
            placeholder = stringResource(Res.string.report_listing_additional_info_placeholder),
            trailingLabel = "${value.length}/$AdditionalInfoMaxLength",
            singleLine = false,
            minLines = 3,
        )
    }
}

@Composable
private fun ReportSubmitBar(
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = 10.dp),
        ) {
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = KupioShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.report_listing_submit),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
