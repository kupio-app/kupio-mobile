package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import kupio.mobile.core.designsystem.borderTop
import kupio.mobile.core.designsystem.bouncingClickable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chat_composer_placeholder
import mobile.composeapp.generated.resources.chat_send
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .borderTop(KupioThemeDefaults.borderWidths.regular, KupioThemeDefaults.navDividerColor),
        color = colors.background,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = KupioShapes.ExtraLarge,
                color = colors.surface,
                border = KupioThemeDefaults.defaultBorder,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    decorationBox = { innerTextField ->
                        if (draft.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.chat_composer_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bouncingClickable(onSend),
                shape = KupioShapes.Large,
                color = colors.primary,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.chat_send),
                    tint = colors.onPrimary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )
            }
        }
    }
}
