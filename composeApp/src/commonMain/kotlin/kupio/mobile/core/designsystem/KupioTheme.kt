package kupio.mobile.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val KupioLightColors = lightColorScheme(
    primary = Color(0xFFB4623A),              // Terracotta accent
    onPrimary = Color(0xFFFDFAF6),            // cream (kp-inverse)
    primaryContainer = Color(0xFFF2E8D8),     // kp-card-2 (warm tint)
    onPrimaryContainer = Color(0xFF3B2A1A),   // kp-ink-1
    secondary = Color(0xFF7A6650),            // kp-ink-2 (mid brown)
    onSecondary = Color(0xFFFDFAF6),          // cream
    background = Color(0xFFF6EFE5),           // kp-bg (warm beige)
    onBackground = Color(0xFF3B2A1A),         // kp-ink-1
    surface = Color(0xFFFFFFFF),              // kp-card (cream)
    onSurface = Color(0xFF3B2A1A),            // kp-ink-1
    surfaceVariant = Color(0xFFF2E8D8),       // kp-card-2
    onSurfaceVariant = Color(0xFF7A6650),     // kp-ink-2
    outline = Color(0xFFA8957B),              // kp-ink-3 (muted taupe)
)

private val KupioDarkColors = darkColorScheme(
    primary = Color(0xFFB4623A),              // Terracotta (holds up in dark)
    onPrimary = Color(0xFFFDFAF6),            // cream
    primaryContainer = Color(0xFF3A2B1D),     // kp-card-2 dark
    onPrimaryContainer = Color(0xFFF3E6D3),   // kp-ink-1 dark
    secondary = Color(0xFFC1A986),            // kp-ink-2 dark (warm sand)
    onSecondary = Color(0xFF1E160E),          // kp-inverse dark
    background = Color(0xFF1E160E),           // kp-bg dark
    onBackground = Color(0xFFF3E6D3),         // kp-ink-1 dark
    surface = Color(0xFF2B2016),              // kp-card dark
    onSurface = Color(0xFFF3E6D3),            // kp-ink-1 dark
    surfaceVariant = Color(0xFF3A2B1D),       // kp-card-2 dark
    onSurfaceVariant = Color(0xFFC1A986),     // kp-ink-2 dark
    outline = Color(0xFF8A7559),              // kp-ink-3 dark
)

@Immutable
data class KupioSpacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp,
)

private val LocalKupioSpacing = staticCompositionLocalOf { KupioSpacing() }

object KupioThemeDefaults {
    val spacing: KupioSpacing
        @Composable get() = LocalKupioSpacing.current
}

@Composable
fun KupioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) {
        KupioDarkColors
    } else {
        KupioLightColors
    }

    // TODO: Replace the starter typography with brand fonts when the final design system is defined.
    val typography = MaterialTheme.typography.copy(
        headlineMedium = TextStyle.Default.copy(fontFamily = FontFamily.SansSerif),
        titleLarge = TextStyle.Default.copy(fontFamily = FontFamily.SansSerif),
        bodyLarge = TextStyle.Default.copy(fontFamily = FontFamily.SansSerif),
    )

    CompositionLocalProvider(
        LocalKupioSpacing provides KupioSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

inline fun Modifier.bouncingClickable(
    crossinline onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    val opacity by animateFloatAsState(
        targetValue = if (pressed) 0.7f else 1f,
        label = "opacity"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = opacity
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

inline fun Modifier.glowClickable(
    shape: Shape = RoundedCornerShape(12.dp), // Match your button's corner radius
    crossinline onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    // Animate the alpha of the highlight
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.15f else 0f,
        label = "GlowAlpha"
    )

    this
        .clip(shape) // Ensures the glow doesn't leak outside the border
        .drawBehind {
            // Draws the highlight color behind the text but inside the border
            if (highlightAlpha > 0f) {
                drawRect(color = Color.White.copy(alpha = highlightAlpha))
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

fun Modifier.borderTop(
    width: androidx.compose.ui.unit.Dp,
    color: Color,
): Modifier = composed {
    this.drawBehind {
        val strokeWidth = width.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )
    }
}

fun Modifier.borderBottom(
    width: androidx.compose.ui.unit.Dp,
    color: Color,
): Modifier = composed {
    this.drawBehind {
        val strokeWidth = width.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth
        )
    }
}
