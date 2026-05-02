package kupio.mobile.features.listings.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.datetime.formatPostedAt
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.presentation.detail.ListingOwnerMetadataUi
import kupio.mobile.features.listings.presentation.detail.label
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_detail_free
import mobile.composeapp.generated.resources.listing_detail_posted
import mobile.composeapp.generated.resources.listing_detail_promoted
import mobile.composeapp.generated.resources.listing_detail_tradable
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingSummarySection(
    listing: Listing,
    ownerMetadata: ListingOwnerMetadataUi?,
    showStatus: Boolean,
    showPostedAt: Boolean,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = listing.categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "ID ${listing.id.take(8)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showStatus) {
                val status = ownerMetadata?.status ?: listing.status
                StatusChip(
                    text = status.label(),
                    isActive = status == ListingStatus.ACTIVE,
                )
            }
            if (showStatus && ownerMetadata?.isPromoted == true) {
                StatusChip(
                    text = stringResource(Res.string.listing_detail_promoted),
                    isPromoted = true,
                )
            }
            if (listing.isFree) {
                StatusChip(text = stringResource(Res.string.listing_detail_free))
            }
            if (listing.isTradable) {
                StatusChip(text = stringResource(Res.string.listing_detail_tradable))
            }
        }
        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = listing.formatPrice(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (showPostedAt) {
            Text(
                text = stringResource(Res.string.listing_detail_posted, listing.createdAt.formatPostedAt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun StatusChip(
    text: String,
    isPromoted: Boolean = false,
    isActive: Boolean = false,
) {
    Surface(
        shape = if (isActive) KupioShapes.Full else KupioShapes.Small,
        color = when {
            isActive -> MaterialTheme.colorScheme.primaryContainer
            isPromoted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (isActive) KupioThemeDefaults.spacing.sm else 8.dp,
                vertical = if (isActive) 2.dp else 3.dp,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isPromoted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium,
        )
    }
}
