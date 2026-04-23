package kupio.mobile.features.create

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.glowClickable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.screen_create_body
import mobile.composeapp.generated.resources.topbar_back
import mobile.composeapp.generated.resources.topbar_draft_autosaved
import mobile.composeapp.generated.resources.topbar_new_listing_title
import mobile.composeapp.generated.resources.topbar_preview
import org.jetbrains.compose.resources.stringResource

class CreateScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = stringResource(Res.string.topbar_new_listing_title),
                    subtitle = stringResource(Res.string.topbar_draft_autosaved),
                    leadingContent = {
                        Icon(
                            modifier = Modifier
                                .size(32.dp)
                                .bouncingClickable { navigator.pop() },
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(Res.string.topbar_back),
                        )
                    },
                    trailingContent = {
                        Surface (
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, shape = RoundedCornerShape(8.dp))
                                .glowClickable(shape = RoundedCornerShape(8.dp)) { },
                            color = Color.Transparent,
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(vertical = KupioThemeDefaults.spacing.sm, horizontal = KupioThemeDefaults.spacing.lg),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = stringResource(Res.string.topbar_preview)
                            )
                        }
                    },
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.screen_create_body))
        }
    }
}
