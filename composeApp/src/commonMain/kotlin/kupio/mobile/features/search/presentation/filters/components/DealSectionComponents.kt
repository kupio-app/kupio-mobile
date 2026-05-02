package kupio.mobile.features.search.presentation.filters.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.presentation.filters.SearchFiltersIntent
import kupio.mobile.features.search.presentation.filters.SearchFiltersState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_filters_deal_for_sale
import mobile.composeapp.generated.resources.search_filters_deal_free
import mobile.composeapp.generated.resources.search_filters_deal_trade
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DealTypeSection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KupioThemeDefaults.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_for_sale),
            selected = state.draft.dealType == DealType.FOR_SALE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.FOR_SALE)) },
            modifier = Modifier.weight(1f),
        )
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_free),
            selected = state.draft.dealType == DealType.FREE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.FREE)) },
            modifier = Modifier.weight(1f),
        )
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_trade),
            selected = state.draft.dealType == DealType.TRADE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.TRADE)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DealTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .bouncingDimClickable(shape = KupioShapes.Large, onClick = onClick),
        shape = KupioShapes.Large,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = if (selected) null else KupioThemeDefaults.strongBorder,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}