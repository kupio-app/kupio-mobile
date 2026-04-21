package kupio.mobile.features.auth.presentation.username

import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.features.auth.presentation.auth.components.AuthCardHeader
import kupio.mobile.features.auth.presentation.auth.components.AuthInlineError
import kupio.mobile.features.auth.presentation.auth.components.AuthPrimaryButton
import kupio.mobile.features.auth.presentation.auth.components.AuthShell
import kupio.mobile.features.auth.presentation.auth.components.AuthTextField
import kupio.mobile.features.auth.presentation.auth.components.AuthViewport
import kupio.mobile.features.auth.presentation.auth.toErrorMessage
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_one_last_step
import mobile.composeapp.generated.resources.auth_username
import mobile.composeapp.generated.resources.auth_username_hint
import mobile.composeapp.generated.resources.auth_username_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
    AuthViewport { metrics ->
        AuthShell(
            metrics = metrics,
            footer = stringResource(Res.string.auth_username_hint),
        ) {
            AuthCardHeader(
                title = stringResource(Res.string.auth_one_last_step),
                supporting = state.email.ifBlank {
                    stringResource(Res.string.auth_username_hint)
                },
            )
            AuthInlineError(message = state.formError)
            AuthTextField(
                value = state.username,
                onValueChange = { onIntent(UsernameIntent.UsernameChanged(it)) },
                label = stringResource(Res.string.auth_username),
                placeholder = stringResource(Res.string.auth_username_placeholder),
                errorMessage = state.usernameError.toErrorMessage(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
            )
            AuthPrimaryButton(
                text = stringResource(Res.string.auth_continue),
                loading = state.isSubmitting,
                enabled = !state.isSubmitting,
                onClick = { onIntent(UsernameIntent.SubmitClicked) },
            )
        }
    }
}
