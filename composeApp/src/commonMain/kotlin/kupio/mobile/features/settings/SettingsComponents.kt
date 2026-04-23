package kupio.mobile.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.preferences.ThemeMode
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.theme_mode_current
import mobile.composeapp.generated.resources.theme_mode_dark
import mobile.composeapp.generated.resources.theme_mode_label
import mobile.composeapp.generated.resources.theme_mode_light
import mobile.composeapp.generated.resources.theme_mode_system
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsThemeModeSection(
    selectedThemeMode: ThemeMode,
    enabled: Boolean,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentModeLabel = selectedThemeMode.toLabel()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
    ) {
        KupioText(text = stringResource(Res.string.theme_mode_label))
        ThemeModeOptionButton(
            text = stringResource(Res.string.theme_mode_system),
            onClick = { onModeSelected(ThemeMode.SYSTEM) },
            enabled = enabled,
        )
        ThemeModeOptionButton(
            text = stringResource(Res.string.theme_mode_light),
            onClick = { onModeSelected(ThemeMode.LIGHT) },
            enabled = enabled,
        )
        ThemeModeOptionButton(
            text = stringResource(Res.string.theme_mode_dark),
            onClick = { onModeSelected(ThemeMode.DARK) },
            enabled = enabled,
        )
        KupioText(
            text = stringResource(Res.string.theme_mode_current, currentModeLabel),
        )
    }
}

@Composable
private fun ThemeModeOptionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    KupioButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun ThemeMode.toLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
        ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
    }
}
