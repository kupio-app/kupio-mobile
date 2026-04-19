package kupio.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val KupioLightColors = lightColorScheme(
    primary = Color(0xFF1664D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E7FF),
    onPrimaryContainer = Color(0xFF001B47),
    secondary = Color(0xFF315EA8),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF151A23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151A23),
    surfaceVariant = Color(0xFFE8EDF7),
    onSurfaceVariant = Color(0xFF434A57),
    outline = Color(0xFF737B8A),
)

private val KupioDarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF00459C),
    onPrimaryContainer = Color(0xFFD9E7FF),
    secondary = Color(0xFFB7CCF4),
    onSecondary = Color(0xFF002C68),
    background = Color(0xFF0F141C),
    onBackground = Color(0xFFE2E8F3),
    surface = Color(0xFF171D27),
    onSurface = Color(0xFFE2E8F3),
    surfaceVariant = Color(0xFF2A303B),
    onSurfaceVariant = Color(0xFFC2C9D6),
    outline = Color(0xFF8C95A5),
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
