package kupio.mobile.features.listings.presentation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState
import kupio.mobile.features.listings.domain.model.Category
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_all_categories
import mobile.composeapp.generated.resources.create_categories
import mobile.composeapp.generated.resources.create_category_choose
import mobile.composeapp.generated.resources.create_category_use
import mobile.composeapp.generated.resources.create_error_load_subcategories
import mobile.composeapp.generated.resources.create_subcategories
import org.jetbrains.compose.resources.stringResource
import kupio.mobile.core.designsystem.KupioShapes

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPickerSheet(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.create_category_choose),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            CategoryBreadcrumb(
                path = state.categoryPath,
                onRootClick = { onIntent(CreateIntent.CategoryPickerReset) },
                onCategoryClick = { onIntent(CreateIntent.CategorySelected(it.id)) },
            )

            if (state.categoryPath.isNotEmpty()) {
                CategoryBackRow(
                    path = state.categoryPath,
                    onClick = { onIntent(CreateIntent.CategoryPickerBack) },
                )
            }

            state.selectedCategoryName?.let { categoryName ->
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = KupioShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(stringResource(Res.string.create_category_use, categoryName))
                }
            }

            val categories = if (state.categoryPath.isEmpty()) {
                state.categories
            } else {
                state.visibleSubcategories
            }

            if (state.categoryPath.isEmpty() || categories.isNotEmpty() || state.isLoadingSubcategories || state.subcategoriesError != null) {
                CategoryGroupLabel(
                    if (state.categoryPath.isEmpty()) {
                        stringResource(Res.string.create_categories)
                    } else {
                        stringResource(Res.string.create_subcategories)
                    },
                )
            }

            when {
                categories.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { category ->
                        CategoryPickerRow(
                            category = category,
                            selected = state.categoryPath.isNotEmpty() && state.selectedCategoryId == category.id,
                            onClick = { onIntent(CreateIntent.CategorySelected(category.id)) },
                        )
                    }
                }
                state.isLoadingSubcategories -> LoadingRow()
                state.subcategoriesError != null -> RetryRow(
                    message = stringResource(Res.string.create_error_load_subcategories),
                    onRetry = { onIntent(CreateIntent.RetrySubcategories) },
                )
            }

            if (state.isLoadingSubcategories && categories.isNotEmpty()) {
                LoadingRow()
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryBackRow(
    path: List<Category>,
    onClick: () -> Unit,
) {
    val previousLayer = path.dropLast(1).lastOrNull()?.name ?: stringResource(Res.string.create_all_categories)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
        shape = KupioShapes.Medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = previousLayer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryBreadcrumb(
    path: List<Category>,
    onRootClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CategoryBreadcrumbItem(
            text = stringResource(Res.string.create_all_categories),
            onClick = onRootClick,
        )
        path.forEach { category ->
            Text(
                text = "/",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 7.dp),
            )
            CategoryBreadcrumbItem(
                text = category.name,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}

@Composable
private fun CategoryBreadcrumbItem(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(KupioShapes.Small)
            .bouncingDimClickable(shape = KupioShapes.Small, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun CategoryGroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun CategoryPickerRow(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
        shape = KupioShapes.Medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            BorderStroke(
                width = KupioThemeDefaults.borderWidths.regular,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            )
        } else {
            KupioThemeDefaults.strongBorder
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(KupioShapes.Full)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.Transparent
                        },
                    )
                    .border(
                        KupioThemeDefaults.strongBorder,
                        KupioShapes.Full,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
