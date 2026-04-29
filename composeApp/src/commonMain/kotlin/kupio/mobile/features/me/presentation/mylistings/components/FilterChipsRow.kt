package kupio.mobile.features.me.presentation.mylistings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import kupio.mobile.core.designsystem.KupioFilterChip
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.me.presentation.mylistings.MyListingsFilter
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_filter_active
import mobile.composeapp.generated.resources.my_listings_filter_all
import mobile.composeapp.generated.resources.my_listings_filter_draft
import mobile.composeapp.generated.resources.my_listings_filter_inactive
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FilterChipsRow(
    selectedFilter: MyListingsFilter,
    onFilterSelected: (MyListingsFilter) -> Unit,
) {
    val filters = listOf(
        MyListingsFilter.ACTIVE to stringResource(Res.string.my_listings_filter_active),
        MyListingsFilter.INACTIVE to stringResource(Res.string.my_listings_filter_inactive),
        MyListingsFilter.DRAFT to stringResource(Res.string.my_listings_filter_draft),
        MyListingsFilter.ALL to stringResource(Res.string.my_listings_filter_all),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
        items(filters) { (filter, label) ->
            KupioFilterChip(
                label = label,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}
