package kupio.mobile.core.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun KupioTopBarIconAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Icon(
        modifier = modifier
            .size(24.dp)
            .bouncingClickable(onClick = onClick),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
    )
}

@Composable
fun KupioTopBarBackAction(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Default.ChevronLeft,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    KupioTopBarIconAction(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun KupioTopBarOutlinedTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .border(KupioThemeDefaults.ghostBorder, shape)
            .glowClickable(shape = shape, onClick = onClick),
        color = Color.Transparent,
    ) {
        Text(
            modifier = Modifier.padding(
                vertical = KupioThemeDefaults.spacing.sm,
                horizontal = KupioThemeDefaults.spacing.lg,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            text = text,
        )
    }
}

