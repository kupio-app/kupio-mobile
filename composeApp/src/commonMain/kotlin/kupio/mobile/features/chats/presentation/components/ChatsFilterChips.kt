package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kupio.mobile.core.designsystem.KupioFilterChip
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.chats.presentation.list.ChatsFilter
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chats_filter
import mobile.composeapp.generated.resources.chats_filter_all
import mobile.composeapp.generated.resources.chats_filter_buying
import mobile.composeapp.generated.resources.chats_filter_selling
import org.jetbrains.compose.resources.stringResource

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
        KupioFilterChip(
            label = stringResource(Res.string.chats_filter_all),
            count = counts[ChatsFilter.ALL] ?: 0,
            selected = selected == ChatsFilter.ALL,
            onClick = { onSelect(ChatsFilter.ALL) },
        )
        KupioFilterChip(
            label = stringResource(Res.string.chats_filter_buying),
            count = counts[ChatsFilter.BUYING] ?: 0,
            selected = selected == ChatsFilter.BUYING,
            onClick = { onSelect(ChatsFilter.BUYING) },
        )
        KupioFilterChip(
            label = stringResource(Res.string.chats_filter_selling),
            count = counts[ChatsFilter.SELLING] ?: 0,
            selected = selected == ChatsFilter.SELLING,
            onClick = { onSelect(ChatsFilter.SELLING) },
        )
    }
}
