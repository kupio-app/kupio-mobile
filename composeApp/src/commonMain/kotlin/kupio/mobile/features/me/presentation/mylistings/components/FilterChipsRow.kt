package kupio.mobile.features.me.presentation.mylistings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.me.presentation.mylistings.MyListingsFilter
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_filter_active
import mobile.composeapp.generated.resources.my_listings_filter_all
import mobile.composeapp.generated.resources.my_listings_filter_draft
import mobile.composeapp.generated.resources.my_listings_filter_inactive
import org.jetbrains.compose.resources.stringResource
import kupio.mobile.core.designsystem.KupioShapes

@Composable
internal fun FilterChipsRow(
    selectedFilter: MyListingsFilter,
    onFilterSelected: (MyListingsFilter) -> Unit,
) {
    val activeLabel = stringResource(Res.string.my_listings_filter_active)
    val inactiveLabel = stringResource(Res.string.my_listings_filter_inactive)
    val draftLabel = stringResource(Res.string.my_listings_filter_draft)
    val allLabel = stringResource(Res.string.my_listings_filter_all)
    val filters = listOf(
        MyListingsFilter.ACTIVE to activeLabel,
        MyListingsFilter.INACTIVE to inactiveLabel,
        MyListingsFilter.DRAFT to draftLabel,
        MyListingsFilter.ALL to allLabel,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
        items(filters) { (filter, label) ->
            FilterChip(
                label = label,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    val bgColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val textColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.bouncingClickable(onClick = onClick),
        shape = KupioShapes.Full,
        color = bgColor,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
    }
}
