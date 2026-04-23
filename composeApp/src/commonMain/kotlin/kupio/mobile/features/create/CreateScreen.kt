package kupio.mobile.features.create

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopBarOutlinedTextAction
import kupio.mobile.core.designsystem.KupioTopNavbar
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
                        KupioTopBarBackAction(
                            contentDescription = stringResource(Res.string.topbar_back),
                            onClick = { navigator.pop() },
                        )
                    },
                    trailingContent = {
                        KupioTopBarOutlinedTextAction(
                            text = stringResource(Res.string.topbar_preview),
                            onClick = { },
                        )
                    },
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.screen_create_body))
        }
    }
}
