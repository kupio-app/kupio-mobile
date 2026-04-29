package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.features.chats.domain.model.ChatRole
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chat_role_buying
import mobile.composeapp.generated.resources.chat_role_selling
import org.jetbrains.compose.resources.stringResource

@Composable
fun RoleChip(
    role: ChatRole,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = KupioShapes.Micro,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            text = when (role) {
                ChatRole.BUYING -> stringResource(Res.string.chat_role_buying)
                ChatRole.SELLING -> stringResource(Res.string.chat_role_selling)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
