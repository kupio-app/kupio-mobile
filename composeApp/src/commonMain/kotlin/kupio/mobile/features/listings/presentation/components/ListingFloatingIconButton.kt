package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.bouncingClickable

@Composable
fun ListingFloatingIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.65f)
            .semantics { this.contentDescription = contentDescription; this.role = Role.Button }
            .let { if (enabled) it.bouncingClickable(onClick) else it },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
