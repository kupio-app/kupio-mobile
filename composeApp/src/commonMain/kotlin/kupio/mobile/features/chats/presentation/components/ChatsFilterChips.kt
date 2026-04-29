package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.chats.presentation.list.ChatsFilter
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chats_filter
import mobile.composeapp.generated.resources.chats_filter_all
import mobile.composeapp.generated.resources.chats_filter_buying
import mobile.composeapp.generated.resources.chats_filter_selling
import org.jetbrains.compose.resources.stringResource
import kupio.mobile.core.designsystem.KupioShapes

@Composable
fun ChatsFilterChips(
    selected: ChatsFilter,
    counts: Map<ChatsFilter, Int>,
    onSelect: (ChatsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            label = stringResource(Res.string.chats_filter_all),
            count = counts[ChatsFilter.ALL] ?: 0,
            selected = selected == ChatsFilter.ALL,
            onClick = { onSelect(ChatsFilter.ALL) },
        )
        FilterChip(
            label = stringResource(Res.string.chats_filter_buying),
            count = counts[ChatsFilter.BUYING] ?: 0,
            selected = selected == ChatsFilter.BUYING,
            onClick = { onSelect(ChatsFilter.BUYING) },
        )
        FilterChip(
            label = stringResource(Res.string.chats_filter_selling),
            count = counts[ChatsFilter.SELLING] ?: 0,
            selected = selected == ChatsFilter.SELLING,
            onClick = { onSelect(ChatsFilter.SELLING) },
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = KupioThemeDefaults.spacing

    Surface(
        modifier = Modifier.bouncingClickable(onClick),
        shape = KupioShapes.ExtraLarge,
        color = if (selected) colors.onSurface else colors.background,
        border = if (selected) null else KupioThemeDefaults.strongBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.surface else colors.onSurface,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.surface.copy(alpha = 0.7f) else colors.onSurfaceVariant,
            )
        }
    }
}
