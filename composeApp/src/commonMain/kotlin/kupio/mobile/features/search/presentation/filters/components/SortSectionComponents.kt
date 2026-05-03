package kupio.mobile.features.search.presentation.filters.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.search.domain.model.SearchSortBy
import kupio.mobile.features.search.presentation.filters.SearchFiltersIntent
import kupio.mobile.features.search.presentation.filters.SearchFiltersState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_filters_sort_newest
import mobile.composeapp.generated.resources.search_filters_sort_price_asc
import mobile.composeapp.generated.resources.search_filters_sort_price_desc
import mobile.composeapp.generated.resources.search_filters_sort_recommended
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SortBySection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val sorts = listOf(
        SearchSortBy.RECOMMENDED to stringResource(Res.string.search_filters_sort_recommended),
        SearchSortBy.NEWEST_FIRST to stringResource(Res.string.search_filters_sort_newest),
        SearchSortBy.PRICE_LOW_HIGH to stringResource(Res.string.search_filters_sort_price_asc),
        SearchSortBy.PRICE_HIGH_LOW to stringResource(Res.string.search_filters_sort_price_desc),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KupioThemeDefaults.spacing.lg),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            sorts.forEachIndexed { index, (sort, label) ->
                SortByRow(
                    label = label,
                    selected = state.draft.sortBy == sort,
                    onClick = { onIntent(SearchFiltersIntent.SortBySelected(sort)) },
                    showDivider = index < sorts.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun SortByRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KupioThemeDefaults.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
            )
        }
    }
}