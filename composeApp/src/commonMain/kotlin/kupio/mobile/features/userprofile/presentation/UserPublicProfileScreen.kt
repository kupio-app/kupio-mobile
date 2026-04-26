package kupio.mobile.features.userprofile.presentation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.chat_profile_placeholder
import mobile.composeapp.generated.resources.chat_profile_title
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource

class UserPublicProfileScreen(
    private val userId: String,
    private val displayLabel: String = "",
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = displayLabel.ifBlank { stringResource(Res.string.chat_profile_title) },
                    leadingContent = {
                        KupioTopBarBackAction(
                            contentDescription = stringResource(Res.string.topbar_back),
                            onClick = { navigator.pop() },
                        )
                    },
                )
            },
        ) {
            KupioText(text = stringResource(Res.string.chat_profile_placeholder))
        }
    }
}
