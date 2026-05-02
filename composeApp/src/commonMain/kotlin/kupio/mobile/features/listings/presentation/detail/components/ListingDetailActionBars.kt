package kupio.mobile.features.listings.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.borderTop
import kupio.mobile.features.listings.presentation.detail.ListingDetailIntent
import kupio.mobile.features.listings.presentation.detail.ListingDetailState
import kupio.mobile.features.listings.presentation.detail.canToggleOwnerStatus
import kupio.mobile.features.listings.presentation.detail.toggleLabel
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_detail_call
import mobile.composeapp.generated.resources.listing_detail_message_seller
import mobile.composeapp.generated.resources.my_listings_edit
import mobile.composeapp.generated.resources.my_listings_extend
import mobile.composeapp.generated.resources.my_listings_promote
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailActionBar(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val canCall = state.seller?.let { !it.isCallsDisabled && !it.phone.isNullOrBlank() } == true
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.borderTop(KupioThemeDefaults.borderWidths.regular, KupioThemeDefaults.navDividerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onIntent(ListingDetailIntent.CallSeller) },
                enabled = canCall,
                shape = KupioShapes.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
                border = KupioThemeDefaults.strongBorder,
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = 13.dp),
            ) {
                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.listing_detail_call))
            }
            Button(
                onClick = { onIntent(ListingDetailIntent.OpenMessageSheet) },
                modifier = Modifier.weight(1f),
                shape = KupioShapes.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                contentPadding = PaddingValues(vertical = 13.dp),
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.listing_detail_message_seller),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun OwnerActionBar(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val listing = state.listing ?: return
    val spacing = KupioThemeDefaults.spacing
    val ownerStatus = state.ownerMetadata?.status ?: listing.status
    val canToggle = ownerStatus.canToggleOwnerStatus() && !state.isUpdatingStatus
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.borderTop(KupioThemeDefaults.borderWidths.regular, KupioThemeDefaults.navDividerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OwnerActionButton(
                label = stringResource(Res.string.my_listings_edit),
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.EditListing) },
                modifier = Modifier.weight(1f),
            )
            OwnerActionButton(
                label = if (state.ownerMetadata?.isPromoted == true) {
                    stringResource(Res.string.my_listings_extend)
                } else {
                    stringResource(Res.string.my_listings_promote)
                },
                icon = { Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.PromoteListing) },
                modifier = Modifier.weight(1f),
                emphasis = true,
            )
            OwnerActionButton(
                label = ownerStatus.toggleLabel(),
                icon = { Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.ToggleOwnerStatus) },
                modifier = Modifier.weight(1f),
                enabled = canToggle,
            )
        }
    }
}

@Composable
private fun OwnerActionButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KupioShapes.Large,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasis) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (emphasis) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        border = if (emphasis) null else KupioThemeDefaults.strongBorder,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 13.dp),
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
