package kupio.mobile.features.search.presentation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.topbar_search
import org.jetbrains.compose.resources.stringResource

data class SearchScreen(val initialQuery: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        KupioScaffold(
            topBar = {
                KupioTopNavbar(
                    title = stringResource(Res.string.topbar_search),
                    leadingContent = {
                        KupioTopBarBackAction(
                            contentDescription = stringResource(Res.string.back),
                            onClick = { navigator.pop() },
                        )
                    },
                )
            },
        ) {
            KupioText(text = initialQuery)
        }
    }
}
