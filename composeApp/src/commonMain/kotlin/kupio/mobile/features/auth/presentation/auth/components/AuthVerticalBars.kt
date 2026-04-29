package kupio.mobile.features.auth.presentation.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.borderBottom
import kupio.mobile.core.designsystem.borderTop
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.standaloneTopBarInsetsPadding
import kupio.mobile.features.auth.presentation.auth.AuthMode

@Composable
fun AuthTopNavbar(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .standaloneTopBarInsetsPadding()
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KupioThemeDefaults.spacing.xl, start = KupioThemeDefaults.spacing.xl, bottom = KupioThemeDefaults.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KupioTopBarBackAction(
                onClick = onBackClick,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Back",
            )
        }
    }
}

@Composable
fun AuthModeFooter(currentMode: AuthMode, onSwitchMode: (AuthMode) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .borderTop(KupioThemeDefaults.borderWidths.thin, KupioThemeDefaults.navDividerColor),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KupioThemeDefaults.spacing.md, bottom = KupioThemeDefaults.spacing.lg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prefix = if (currentMode == AuthMode.LOGIN) "Don't have an account?" else "Already have an account?"
                val linkText = if (currentMode == AuthMode.LOGIN) "Create one →" else "Log in →"
                val nextMode = if (currentMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN

                Text(
                    text = "$prefix ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = linkText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.bouncingClickable { onSwitchMode(nextMode) }
                )
            }
        }
    }
}