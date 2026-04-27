package kupio.mobile.features.me.presentation.mylistings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.formatPrice
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_activate
import mobile.composeapp.generated.resources.my_listings_boost
import mobile.composeapp.generated.resources.my_listings_bump_up
import mobile.composeapp.generated.resources.my_listings_deactivate
import mobile.composeapp.generated.resources.my_listings_edit
import mobile.composeapp.generated.resources.my_listings_filter_active
import mobile.composeapp.generated.resources.my_listings_extend
import mobile.composeapp.generated.resources.my_listings_in_n_days
import mobile.composeapp.generated.resources.my_listings_promote
import mobile.composeapp.generated.resources.my_listings_promoted_label
import mobile.composeapp.generated.resources.my_listings_stats_icon_desc
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OwnedListingCard(
    listing: OwnedListing,
    onEdit: () -> Unit,
    onBumpUp: () -> Unit,
    onPromote: () -> Unit,
    onToggleActive: (deactivate: Boolean) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val isActive = listing.status == OwnedListingStatus.ACTIVE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                ListingImagePlaceholder(listing.id)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listing.formatPrice(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ListingStatsRow(
                        seenCount = listing.seenCount,
                        favouritesCount = listing.favouritesCount,
                        chatsCount = listing.chatsCount,
                    )
                }
                Spacer(modifier = Modifier.width(spacing.xs))
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .bouncingClickable(onClick = onEdit),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(Res.string.my_listings_edit),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (listing.isPromoted) {
                PromotionBanner(listing = listing)
            }

            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)

            ListingActionButtons(
                isActive = isActive,
                isPromoted = listing.isPromoted,
                onBumpUp = onBumpUp,
                onPromote = onPromote,
                onToggleActive = { onToggleActive(isActive) },
            )
        }
    }
}

@Composable
private fun ListingImagePlaceholder(listingId: String) {
    val colors = listOf(
        Color(0xFFCEB99A),
        Color(0xFFB5A088),
        Color(0xFF9B876F),
        Color(0xFF856E58),
    )
    val bg = colors[(listingId.hashCode() and 0x7FFFFFFF) % colors.size]
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(bg, RoundedCornerShape(10.dp)),
    )
}

@Composable
private fun ListingStatsRow(seenCount: Int, favouritesCount: Int, chatsCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(icon = Icons.Outlined.Visibility, count = seenCount)
        StatItem(icon = Icons.Outlined.FavoriteBorder, count = favouritesCount)
        StatItem(icon = Icons.Default.ChatBubbleOutline, count = chatsCount)
    }
}

@Composable
private fun StatItem(icon: ImageVector, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(Res.string.my_listings_stats_icon_desc),
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PromotionBanner(listing: OwnedListing) {
    val spacing = KupioThemeDefaults.spacing
    val daysLeft = promotionDaysLeft(listing.promotionExpiresAt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(
            text = if (daysLeft != null)
                stringResource(Res.string.my_listings_promoted_label, daysLeft)
            else
                stringResource(Res.string.my_listings_filter_active),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.my_listings_boost, 3),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListingActionButtons(
    isActive: Boolean,
    isPromoted: Boolean,
    onBumpUp: () -> Unit,
    onPromote: () -> Unit,
    onToggleActive: () -> Unit,
) {
    val dividerColor = KupioThemeDefaults.softDividerColor
    Row(modifier = Modifier.fillMaxWidth()) {
        ActionButton(
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.my_listings_bump_up),
            subtitle = stringResource(Res.string.my_listings_in_n_days, 2),
            color = MaterialTheme.colorScheme.onSurface,
            onClick = onBumpUp,
            showDividerAfter = true,
            dividerColor = dividerColor,
            icon = Icons.Outlined.KeyboardArrowUp,
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            label = if (isPromoted)
                stringResource(Res.string.my_listings_extend)
            else
                stringResource(Res.string.my_listings_promote),
            subtitle = null,
            color = MaterialTheme.colorScheme.primary,
            onClick = onPromote,
            showDividerAfter = true,
            dividerColor = dividerColor,
            icon = Icons.Outlined.Bolt
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            label = if (isActive)
                stringResource(Res.string.my_listings_deactivate)
            else
                stringResource(Res.string.my_listings_activate),
            subtitle = null,
            color = MaterialTheme.colorScheme.onSurface,
            onClick = onToggleActive,
            showDividerAfter = false,
            dividerColor = dividerColor,
            icon = Icons.Outlined.Circle
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    subtitle: String?,
    color: Color,
    onClick: () -> Unit,
    icon: ImageVector,
    showDividerAfter: Boolean,
    dividerColor: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .bouncingClickable(onClick = onClick)
                .padding(vertical = spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = color,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDividerAfter) {
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(40.dp)
                    .align(Alignment.CenterVertically)
                    .background(dividerColor)
            )
        }
    }
}

private fun promotionDaysLeft(expiresAt: String?): Int? {
    if (expiresAt == null) return null
    return try {
        expiresAt.substring(8, 10).toInt()
    } catch (_: Throwable) {
        null
    }
}
