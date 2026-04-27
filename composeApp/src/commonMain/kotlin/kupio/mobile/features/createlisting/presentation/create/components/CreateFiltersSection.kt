package kupio.mobile.features.createlisting.presentation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.createlisting.presentation.create.CreateFilterInput
import kupio.mobile.features.createlisting.presentation.create.CreateIntent
import kupio.mobile.features.createlisting.presentation.create.CreateState
import kupio.mobile.features.createlisting.presentation.create.numberPlaceholder
import kupio.mobile.features.createlisting.presentation.create.numericText
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType

@Composable
internal fun FiltersSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Category filters") {
        when {
            state.selectedCategoryId == null -> Text(
                text = "Select a category to see available filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.isLoadingFilters -> LoadingRow()
            state.filtersError != null -> RetryRow(
                message = "Could not load category filters.",
                onRetry = { onIntent(CreateIntent.RetryFilters) },
            )

            state.filters.isEmpty() -> Text(
                text = "No extra details needed for this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> state.filters.forEach { filter ->
                FilterInput(
                    filter = filter,
                    value = state.filterValues[filter.slug],
                    error = state.filterErrors[filter.slug],
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun FilterInput(
    filter: FilterDefinition,
    value: CreateFilterInput?,
    error: String?,
    onIntent: (CreateIntent) -> Unit,
) {
    when (filter.type) {
        FilterType.TEXT -> CreateTextField(
            label = filter.label,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            onValueChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it)) },
            placeholder = filter.label,
            error = error,
            required = filter.isRequired,
            singleLine = true,
        )

        FilterType.NUMBER, FilterType.RANGE -> CreateTextField(
            label = filter.label,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            onValueChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it.numericText())) },
            placeholder = filter.numberPlaceholder(),
            error = error,
            required = filter.isRequired,
            singleLine = true,
            keyboardType = KeyboardType.Decimal,
        )

        FilterType.BOOLEAN -> BooleanFilter(
            filter = filter,
            value = (value as? CreateFilterInput.BooleanValue)?.value,
            error = error,
            onChange = { onIntent(CreateIntent.FilterBooleanChanged(filter.slug, it)) },
        )

        FilterType.SELECT -> SelectFilter(
            filter = filter,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            error = error,
            onChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectFilter(
    filter: FilterDefinition,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    FieldLabel(
        label = filter.label,
        required = filter.isRequired,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filter.options.values.forEach { option ->
            ChoiceChip(
                selected = value == option,
                onClick = { onChange(option) },
                text = option,
            )
        }
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun BooleanFilter(
    filter: FilterDefinition,
    value: Boolean?,
    error: String?,
    onChange: (Boolean?) -> Unit,
) {
    FieldLabel(
        label = filter.label,
        required = filter.isRequired,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!filter.isRequired) {
            ChoiceChip(
                selected = value == null,
                onClick = { onChange(null) },
                text = "Unset",
            )
        }
        ChoiceChip(
            selected = value == true,
            onClick = { onChange(true) },
            text = "Yes",
        )
        ChoiceChip(
            selected = value == false,
            onClick = { onChange(false) },
            text = "No",
        )
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .bouncingDimClickable(shape = RoundedCornerShape(99.dp), onClick = onClick),
        shape = RoundedCornerShape(99.dp),
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            },
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
