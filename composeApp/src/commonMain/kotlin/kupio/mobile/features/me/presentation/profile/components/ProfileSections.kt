package kupio.mobile.features.me.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.me.domain.model.UserListingStats
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.profile_active
import mobile.composeapp.generated.resources.profile_activity
import mobile.composeapp.generated.resources.profile_chats
import mobile.composeapp.generated.resources.profile_edit
import mobile.composeapp.generated.resources.profile_favourites
import mobile.composeapp.generated.resources.profile_inactive
import mobile.composeapp.generated.resources.profile_manage
import mobile.composeapp.generated.resources.profile_my_listings
import mobile.composeapp.generated.resources.profile_payments
import mobile.composeapp.generated.resources.profile_payments_history
import mobile.composeapp.generated.resources.profile_promoted
import mobile.composeapp.generated.resources.profile_promotions_packages
import mobile.composeapp.generated.resources.profile_reports_dashboard
import mobile.composeapp.generated.resources.profile_reports_open_items
import mobile.composeapp.generated.resources.profile_section
import mobile.composeapp.generated.resources.profile_settings
import mobile.composeapp.generated.resources.profile_topup_balance
import org.jetbrains.compose.resources.stringResource

@Composable
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun MyListingsSection(stats: UserListingStats?, onManageClick: () -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.profile_my_listings),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .bouncingClickable(onClick = onManageClick)
                    .testTag("profile.manage-listings"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.profile_manage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            ListingStatCard(
                modifier = Modifier.weight(1f),
                count = stats?.activeCount ?: 0,
                label = stringResource(Res.string.profile_active),
                onClick = onManageClick,
            )
            ListingStatCard(
                modifier = Modifier.weight(1f),
                count = stats?.inactiveCount ?: 0,
                label = stringResource(Res.string.profile_inactive),
                onClick = onManageClick,
            )
            ListingStatCard(
                modifier = Modifier.weight(1f),
                count = stats?.promotedCount ?: 0,
                label = stringResource(Res.string.profile_promoted),
                onClick = onManageClick,
                showBolt = true,
            )
        }
    }
}

@Composable
private fun ListingStatCard(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBolt: Boolean = false,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = modifier.bouncingDimClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (showBolt) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ActivitySection(
    stats: UserListingStats?,
    onChatsClick: () -> Unit,
    onFavouritesClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        SectionLabel(text = stringResource(Res.string.profile_activity))
        MenuGroup {
            ActivityRow(
                icon = Icons.AutoMirrored.Outlined.Message,
                label = stringResource(Res.string.profile_chats),
                count = stats?.chatsCount,
                onClick = onChatsClick,
                modifier = Modifier.testTag("profile.chats"),
            )
            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
            ActivityRow(
                icon = Icons.Outlined.FavoriteBorder,
                label = stringResource(Res.string.profile_favourites),
                count = stats?.favouritesCount,
                onClick = onFavouritesClick,
                modifier = Modifier.testTag("profile.favourites"),
            )
        }
    }
}

@Composable
internal fun PaymentsSection(
    onTopUpClick: () -> Unit,
    onPaymentsHistoryClick: () -> Unit,
    onPromotionsClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        SectionLabel(text = stringResource(Res.string.profile_payments))
        MenuGroup {
            MenuRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = stringResource(Res.string.profile_topup_balance),
                onClick = onTopUpClick,
                modifier = Modifier.testTag("profile.top-up-balance"),
            )
            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
            MenuRow(
                icon = Icons.Outlined.Payments,
                label = stringResource(Res.string.profile_payments_history),
                onClick = onPaymentsHistoryClick,
                modifier = Modifier.testTag("profile.payments-history"),
            )
            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
            MenuRow(
                icon = Icons.Outlined.Bolt,
                label = stringResource(Res.string.profile_promotions_packages),
                onClick = onPromotionsClick,
                modifier = Modifier.testTag("profile.promotions"),
            )
        }
    }
}

@Composable
internal fun ProfileSection(
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        SectionLabel(text = stringResource(Res.string.profile_section))
        MenuGroup {
            MenuRow(
                icon = Icons.Outlined.Edit,
                label = stringResource(Res.string.profile_edit),
                onClick = onEditProfileClick,
                modifier = Modifier.testTag("profile.edit"),
            )
            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
            MenuRow(
                icon = Icons.Outlined.Settings,
                label = stringResource(Res.string.profile_settings),
                onClick = onSettingsClick,
                modifier = Modifier.testTag("profile.settings"),
            )
        }
    }
}

@Composable
internal fun ReportsDashboardCard(openCount: Int, onClick: () -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Surface(
                shape = KupioShapes.Small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.profile_reports_dashboard),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(Res.string.profile_reports_open_items, openCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
            if (openCount > 0) {
                Surface(
                    shape = KupioShapes.Large,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                        text = openCount.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}
