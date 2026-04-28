package kupio.mobile.features.createlisting.presentation.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.createlisting.presentation.create.CreateField
import kupio.mobile.features.createlisting.presentation.create.CreateIntent
import kupio.mobile.features.createlisting.presentation.create.CreateState
import kupio.mobile.features.createlisting.presentation.create.createText
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_category
import mobile.composeapp.generated.resources.create_category_placeholder
import mobile.composeapp.generated.resources.create_category_tap_to_change
import mobile.composeapp.generated.resources.create_description
import mobile.composeapp.generated.resources.create_description_placeholder
import mobile.composeapp.generated.resources.create_details
import mobile.composeapp.generated.resources.create_error_load_categories
import mobile.composeapp.generated.resources.create_title
import mobile.composeapp.generated.resources.create_title_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = stringResource(Res.string.create_details)) {
        CreateTextField(
            label = stringResource(Res.string.create_title),
            value = state.title,
            onValueChange = { onIntent(CreateIntent.TitleChanged(it)) },
            placeholder = stringResource(Res.string.create_title_placeholder),
            error = state.fieldErrors[CreateField.TITLE],
            characterCount = "${state.title.length}/255",
            singleLine = true,
        )
        CreateTextField(
            label = stringResource(Res.string.create_description),
            value = state.description,
            onValueChange = { onIntent(CreateIntent.DescriptionChanged(it)) },
            placeholder = stringResource(Res.string.create_description_placeholder),
            error = state.fieldErrors[CreateField.DESCRIPTION],
            characterCount = "${state.description.length}/5000",
            minLines = 4,
        )
        CategorySelector(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun CategorySelector(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    var showCategoryPicker by remember { mutableStateOf(false) }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            state = state,
            onIntent = onIntent,
            onDismiss = { showCategoryPicker = false },
        )
    }

    FieldLabel(
        label = stringResource(Res.string.create_category),
        required = true,
    )
    when {
        state.isLoadingCategories -> LoadingRow()
        state.categoriesError != null -> RetryRow(
            message = createText(Res.string.create_error_load_categories),
            onRetry = { onIntent(CreateIntent.RetryCategories) },
        )

        else -> {
            CategoryField(
                state = state,
                onClick = { showCategoryPicker = true },
            )
        }
    }
    state.fieldErrors[CreateField.CATEGORY]?.let { ErrorText(it) }
}

@Composable
private fun CategoryField(
    state: CreateState,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(shape = RoundedCornerShape(12.dp), onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.categoryDisplayText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.selectedCategoryId == null) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium,
                )
                if (state.selectedCategoryId != null) {
                    Text(
                        text = stringResource(Res.string.create_category_tap_to_change),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateState.categoryDisplayText(): String =
    when {
        categoryPath.isNotEmpty() -> categoryPath.joinToString(" / ") { it.name }
        selectedCategoryName != null -> selectedCategoryName
        else -> stringResource(Res.string.create_category_placeholder)
    }
