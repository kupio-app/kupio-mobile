package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.offline.OfflineSyncState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_sync_failed
import mobile.composeapp.generated.resources.listing_sync_pending
import mobile.composeapp.generated.resources.listing_sync_syncing
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingSyncBadge(
    syncState: OfflineSyncState,
    modifier: Modifier = Modifier,
) {
    if (syncState == OfflineSyncState.SYNCED) return

    val label = when (syncState) {
        OfflineSyncState.PENDING -> stringResource(Res.string.listing_sync_pending)
        OfflineSyncState.SYNCING -> stringResource(Res.string.listing_sync_syncing)
        OfflineSyncState.FAILED -> stringResource(Res.string.listing_sync_failed)
        OfflineSyncState.SYNCED -> return
    }
    val color = when (syncState) {
        OfflineSyncState.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (syncState) {
        OfflineSyncState.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier,
        shape = KupioShapes.Full,
        color = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
        )
    }
}
