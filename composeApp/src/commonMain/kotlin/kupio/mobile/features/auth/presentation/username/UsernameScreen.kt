package kupio.mobile.features.auth.presentation.username

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.features.auth.presentation.auth.components.AuthHeadlineText
import kupio.mobile.features.auth.presentation.auth.components.AuthInlineError
import kupio.mobile.features.auth.presentation.auth.components.AuthViewport
import kupio.mobile.features.auth.presentation.auth.components.AuthHero
import kupio.mobile.features.auth.presentation.auth.toErrorMessage
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import kupio.mobile.features.auth.presentation.auth.components.AuthActionButton
import org.koin.compose.viewmodel.koinViewModel
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_one_last_step
import mobile.composeapp.generated.resources.auth_username
import mobile.composeapp.generated.resources.auth_username_placeholder
import org.jetbrains.compose.resources.stringResource

class UsernameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<UsernameViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        UsernameContent(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun UsernameContent(
    state: UsernameState,
    onIntent: (UsernameIntent) -> Unit,
) {
    AuthViewport {
        AuthHero()
        Spacer(modifier = Modifier.height(18.dp))
        AuthHeadlineText(
            text = stringResource(Res.string.auth_one_last_step),
        )
        AuthInlineError(message = state.formError.toErrorMessage())
        
        Spacer(modifier = Modifier.height(24.dp))
        
        KupioTextField(
            value = state.username,
            onValueChange = { onIntent(UsernameIntent.UsernameChanged(it)) },
            label = stringResource(Res.string.auth_username),
            placeholder = stringResource(Res.string.auth_username_placeholder),
            error = state.usernameError.toErrorMessage(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        AuthActionButton(
            text = stringResource(Res.string.auth_continue),
            onClick = { onIntent(UsernameIntent.SubmitClicked) },
            enabled = !state.isSubmitting,
            loading = state.isSubmitting,
        )
    }
}
