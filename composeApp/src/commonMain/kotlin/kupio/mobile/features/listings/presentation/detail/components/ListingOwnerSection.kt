package kupio.mobile.features.listings.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.listings.presentation.detail.ListingOwnerMetadataUi
import kupio.mobile.features.listings.presentation.detail.SectionLabel
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_detail_chats
import mobile.composeapp.generated.resources.listing_detail_favourites
import mobile.composeapp.generated.resources.listing_detail_metrics
import mobile.composeapp.generated.resources.listing_detail_status_error
import mobile.composeapp.generated.resources.listing_detail_views
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OwnerMetricsSection(
    ownerMetadata: ListingOwnerMetadataUi?,
    fallbackSeenCount: Int,
) {
    val spacing = KupioThemeDefaults.spacing
    val metrics = if (ownerMetadata == null) {
        listOf(
            OwnerMetricUi(
                count = fallbackSeenCount,
                label = stringResource(Res.string.listing_detail_views),
                icon = Icons.Outlined.Visibility,
            ),
        )
    } else {
        listOf(
            OwnerMetricUi(
                count = ownerMetadata.seenCount,
                label = stringResource(Res.string.listing_detail_views),
                icon = Icons.Outlined.Visibility,
            ),
            OwnerMetricUi(
                count = ownerMetadata.favouritesCount,
                label = stringResource(Res.string.listing_detail_favourites),
                icon = Icons.Outlined.FavoriteBorder,
            ),
            OwnerMetricUi(
                count = ownerMetadata.chatsCount,
                label = stringResource(Res.string.listing_detail_chats),
                icon = Icons.AutoMirrored.Outlined.Message,
            ),
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionLabel(stringResource(Res.string.listing_detail_metrics))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = KupioShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = KupioThemeDefaults.defaultBorder,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                metrics.forEachIndexed { index, metric ->
                    OwnerMetricCell(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                    )
                    if (index < metrics.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(KupioThemeDefaults.borderWidths.thin)
                                .height(108.dp)
                                .background(KupioThemeDefaults.softDividerColor),
                        )
                    }
                }
            }
        }
    }
}

private data class OwnerMetricUi(
    val count: Int,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun OwnerMetricCell(
    metric: OwnerMetricUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = KupioThemeDefaults.spacing.md,
            vertical = KupioThemeDefaults.spacing.md,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = metric.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = metric.count.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = metric.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OwnerStatusError(message: String) {
    val text = message.ifBlank { stringResource(Res.string.listing_detail_status_error) }
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.lg),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
