package kupio.mobile.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.custom_filter_boolean_no
import mobile.composeapp.generated.resources.custom_filter_boolean_unset
import mobile.composeapp.generated.resources.custom_filter_boolean_yes
import mobile.composeapp.generated.resources.custom_filter_number_at_least
import mobile.composeapp.generated.resources.custom_filter_number_range
import mobile.composeapp.generated.resources.custom_filter_number_up_to
import org.jetbrains.compose.resources.stringResource


@Composable
internal fun KupioCustomFilterInput(
    filter: FilterDefinition,
    textValue: String?,
    booleanValue: Boolean?,
    error: String?,
    isValidationEnabled: Boolean = true,
    onTextChanged: (String, String) -> Unit,
    onBooleanChanged: (String, Boolean?) -> Unit,
) {
    when (filter.type) {

        FilterType.TEXT -> KupioTextField(
            value = textValue.orEmpty(),
            onValueChange = { onTextChanged(filter.slug, it) },
            label = filter.label,
            placeholder = filter.label,
            error = error,
            isValidationEnabled = isValidationEnabled,
            required = filter.isRequired,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            minLines = 1,
        )

        FilterType.NUMBER, FilterType.RANGE -> KupioTextField(
            value = textValue.orEmpty(),
            onValueChange = { onTextChanged(filter.slug, it.numericText()) },
            label = filter.label,
            placeholder = filter.numberPlaceholder(),
            error = error,
            isValidationEnabled = isValidationEnabled,
            required = filter.isRequired,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            minLines = 1,
        )

        FilterType.BOOLEAN -> BooleanFilter(
            filter = filter,
            value = booleanValue,
            error = error,
            isValidationEnabled = isValidationEnabled,
            onChange = { onBooleanChanged(filter.slug, it) },
        )

        FilterType.SELECT -> SelectFilter(
            filter = filter,
            value = textValue.orEmpty(),
            error = error,
            isValidationEnabled = isValidationEnabled,
            onChange = { onTextChanged(filter.slug, it) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectFilter(
    filter: FilterDefinition,
    value: String,
    error: String?,
    isValidationEnabled: Boolean,
    onChange: (String) -> Unit,
) {
    KupioFieldLabel(
        label = filter.label,
        required = filter.isRequired,
        withValidationInfo = isValidationEnabled
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
    error.takeIf { isValidationEnabled }?.let { KupioErrorText(it) }
}

@Composable
private fun BooleanFilter(
    filter: FilterDefinition,
    value: Boolean?,
    error: String?,
    isValidationEnabled: Boolean,
    onChange: (Boolean?) -> Unit,
) {
    KupioFieldLabel(
        label = filter.label,
        required = filter.isRequired,
        withValidationInfo = isValidationEnabled,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!filter.isRequired) {
            ChoiceChip(
                selected = value == null,
                onClick = { onChange(null) },
                text = stringResource(Res.string.custom_filter_boolean_unset),
            )
        }
        ChoiceChip(
            selected = value == true,
            onClick = { onChange(true) },
            text = stringResource(Res.string.custom_filter_boolean_yes),
        )
        ChoiceChip(
            selected = value == false,
            onClick = { onChange(false) },
            text = stringResource(Res.string.custom_filter_boolean_no),
        )
    }
    error.takeIf { isValidationEnabled }?.let { KupioErrorText(it) }
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
            .bouncingDimClickable(shape = KupioShapes.Full, onClick = onClick),
        shape = KupioShapes.Full,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(
                width = KupioThemeDefaults.borderWidths.regular,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            KupioThemeDefaults.strongBorder
        },
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

@Composable
private fun FilterDefinition.numberPlaceholder(): String {
    val min = options.min?.formatForDisplay()
    val max = options.max?.formatForDisplay()
    return when {
        min != null && max != null -> stringResource(Res.string.custom_filter_number_range, min, max)
        min != null -> stringResource(Res.string.custom_filter_number_at_least, min)
        max != null -> stringResource(Res.string.custom_filter_number_up_to, max)
        else -> label
    }
}

internal fun Double.formatForDisplay(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

internal fun String.numericText(): String = filterIndexed { index, char ->
    char.isDigit() || char == '.' || (char == '-' && index == 0)
}
