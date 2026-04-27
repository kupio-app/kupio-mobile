package kupio.mobile.features.createlisting.presentation.create.components

import androidx.compose.foundation.BorderStroke
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
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.createlisting.presentation.create.CreateField
import kupio.mobile.features.createlisting.presentation.create.CreateIntent
import kupio.mobile.features.createlisting.presentation.create.CreateState
import kupio.mobile.features.createlisting.presentation.create.categoryDisplayText

@Composable
internal fun DetailsSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Details") {
        CreateTextField(
            label = "Title",
            value = state.title,
            onValueChange = { onIntent(CreateIntent.TitleChanged(it)) },
            placeholder = "What are you selling?",
            error = state.fieldErrors[CreateField.TITLE],
            characterCount = "${state.title.length}/255",
            singleLine = true,
        )
        CreateTextField(
            label = "Description",
            value = state.description,
            onValueChange = { onIntent(CreateIntent.DescriptionChanged(it)) },
            placeholder = "Condition, dimensions, reason for selling...",
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
        label = "Category",
        required = true,
    )
    when {
        state.isLoadingCategories -> LoadingRow()
        state.categoriesError != null -> RetryRow(
            message = "Could not load categories.",
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
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
                        text = "Tap to change",
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
