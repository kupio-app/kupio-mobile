package kupio.mobile.features.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.home.presentation.HomeCategoryItem
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.home_categories_error
import org.jetbrains.compose.resources.stringResource

@Composable
fun CategoriesRow(
    items: List<HomeCategoryItem>,
    selectedId: String,
    isLoading: Boolean,
    error: String?,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(items, key = { it.id }) { item ->
            CategoryChip(
                item = item,
                isSelected = item.id == selectedId,
                onClick = { onSelect(item.id) },
            )
        }

        if (isLoading && items.size <= 1) {
            items(4) {
                SkeletonCategoryChip()
            }
        }

        if (error != null && items.size <= 1) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Spacer(Modifier.height(spacing.md))
                    Text(
                        text = stringResource(Res.string.home_categories_error),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    item: HomeCategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .width(64.dp)
            .bouncingClickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.xs),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) colors.onSurface else colors.surface,
            border = if (!isSelected) KupioThemeDefaults.strongBorder else null,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) colors.surface else colors.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SkeletonCategoryChip() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                ),
        )
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(12.dp)
                .background(
                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                ),
        )
    }
}
