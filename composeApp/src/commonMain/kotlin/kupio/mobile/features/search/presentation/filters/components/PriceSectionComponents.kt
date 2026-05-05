package kupio.mobile.features.search.presentation.filters.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kupio.mobile.core.designsystem.KupioRangeSlider
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.search.presentation.filters.SearchFiltersIntent
import kupio.mobile.features.search.presentation.filters.SearchFiltersState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_filters_price_from
import mobile.composeapp.generated.resources.search_filters_price_range
import mobile.composeapp.generated.resources.search_filters_price_range_invalid
import mobile.composeapp.generated.resources.search_filters_price_to
import org.jetbrains.compose.resources.stringResource

private const val PRICE_MAX_VALUE = 10000f


@Composable
internal fun PriceRangeSection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val isInvalidRange = state.isPriceRangeInvalid
    val minValue = state.draft.minPrice?.toFloat() ?: 0f
    val maxValue = state.draft.maxPrice?.toFloat() ?: PRICE_MAX_VALUE
    val sliderStart = min(minValue, maxValue)
    val sliderEnd = max(minValue, maxValue)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.draft.minPrice != null || state.draft.maxPrice != null) {
            Text(
                text = stringResource(
                    Res.string.search_filters_price_range,
                    state.draft.minPrice ?: 0,
                    state.draft.maxPrice ?: PRICE_MAX_VALUE.toInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PriceInputField(
                label = stringResource(Res.string.search_filters_price_from),
                value = state.draft.minPrice?.toString() ?: "",
                onValueChange = { onIntent(SearchFiltersIntent.PriceMinChanged(it)) },
                isError = isInvalidRange,
                modifier = Modifier.weight(1f),
            )
            PriceInputField(
                label = stringResource(Res.string.search_filters_price_to),
                value = state.draft.maxPrice?.toString() ?: "",
                onValueChange = { onIntent(SearchFiltersIntent.PriceMaxChanged(it)) },
                isError = isInvalidRange,
                modifier = Modifier.weight(1f),
            )
        }

        if (isInvalidRange) {
            Text(
                text = stringResource(Res.string.search_filters_price_range_invalid),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        KupioRangeSlider(
            startValue = sliderStart,
            endValue = sliderEnd,
            valueRange = 0f..PRICE_MAX_VALUE,
            onValueChange = { start, end ->
                onIntent(SearchFiltersIntent.PriceRangeChanged(start.toInt(), end.toInt()))
            },
            isError = isInvalidRange,
        )
    }
}

@Composable
private fun PriceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.Companion,
    isError: Boolean = false,
) {
    val border = if (isError) {
        BorderStroke(KupioThemeDefaults.borderWidths.thin, MaterialTheme.colorScheme.error)
    } else {
        KupioThemeDefaults.defaultBorder
    }
    Surface(
        modifier = modifier.height(52.dp),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}