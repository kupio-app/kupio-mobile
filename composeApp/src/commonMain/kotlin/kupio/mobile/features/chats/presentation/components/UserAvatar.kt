package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val avatarPalette = listOf(
    Color(0xFFCEB99A),
    Color(0xFFB5A088),
    Color(0xFF9B876F),
    Color(0xFF856E58),
    Color(0xFFA8957B),
    Color(0xFF7A6650),
)

@Composable
fun UserAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colorIndex = (initials.hashCode() and 0x7FFFFFFF) % avatarPalette.size
    val bgColor = avatarPalette[colorIndex]

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = bgColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials.take(2),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
