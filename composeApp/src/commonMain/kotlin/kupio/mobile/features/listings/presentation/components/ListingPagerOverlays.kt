package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_detail_photo_count
import org.jetbrains.compose.resources.stringResource

@Composable
fun PagerDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(pageCount) { index ->
            Surface(
                modifier = Modifier.size(width = if (index == currentPage) 20.dp else 6.dp, height = 6.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = if (index == currentPage) 1f else 0.6f,
                ),
                content = {},
            )
        }
    }
}

@Composable
fun ImageCountBadge(
    currentPage: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = modifier,
        shape = KupioShapes.Small,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.surface,
            )
            Text(
                text = stringResource(Res.string.listing_detail_photo_count, currentPage + 1, totalCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
