package kupio.mobile.features.auth.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.auth.presentation.auth.components.AuthCardHeader
import kupio.mobile.features.auth.presentation.auth.components.AuthDivider
import kupio.mobile.features.auth.presentation.auth.components.AuthGoogleButton
import kupio.mobile.features.auth.presentation.auth.components.AuthInlineError
import kupio.mobile.features.auth.presentation.auth.components.AuthModeSelector
import kupio.mobile.features.auth.presentation.auth.components.AuthPrimaryButton
import kupio.mobile.features.auth.presentation.auth.components.AuthShell
import kupio.mobile.features.auth.presentation.auth.components.AuthTextField
import kupio.mobile.features.auth.presentation.auth.components.AuthViewport
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_create_account
import mobile.composeapp.generated.resources.auth_email
import mobile.composeapp.generated.resources.auth_email_placeholder
import mobile.composeapp.generated.resources.auth_form_supporting
import mobile.composeapp.generated.resources.auth_form_title
import mobile.composeapp.generated.resources.auth_google
import mobile.composeapp.generated.resources.auth_login
import mobile.composeapp.generated.resources.auth_or
import mobile.composeapp.generated.resources.auth_password
import mobile.composeapp.generated.resources.auth_password_placeholder
import mobile.composeapp.generated.resources.auth_register
import mobile.composeapp.generated.resources.auth_username
import mobile.composeapp.generated.resources.auth_username_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class AuthScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<AuthViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val googleSignInLauncher = rememberGoogleSignInLauncher()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                AuthEffect.LaunchGoogleSignIn -> {
                    googleSignInLauncher.signIn(
                        callback = object : GoogleSignInCallback {
                            override fun onSuccess(idToken: String) {
                                viewModel.onIntent(AuthIntent.GoogleSuccess(idToken))
                            }

                            override fun onFailure(message: String) {
                                viewModel.onIntent(AuthIntent.GoogleFailure(message))
                            }

                            override fun onCancelled() {
                                viewModel.onIntent(AuthIntent.GoogleCancelled)
                            }
                        },
                    )
                }
            }
        }

        AuthContent(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun AuthContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
) {
    val submitLabel = when (state.mode) {
        AuthMode.LOGIN -> stringResource(Res.string.auth_continue)
        AuthMode.REGISTER -> stringResource(Res.string.auth_create_account)
    }

    AuthViewport { metrics ->
        AuthShell(
            metrics = metrics,
        ) {
            AuthModeSelector(
                selectedMode = state.mode,
                loginLabel = stringResource(Res.string.auth_login),
                registerLabel = stringResource(Res.string.auth_register),
                metrics = metrics,
                onModeSelected = { onIntent(AuthIntent.ModeSelected(it)) },
            )
            AuthCardHeader(
                title = stringResource(Res.string.auth_form_title),
                supporting = stringResource(Res.string.auth_form_supporting),
            )
            AuthInlineError(message = state.formError)
            AuthTextField(
                value = state.email,
                onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                label = stringResource(Res.string.auth_email),
                placeholder = stringResource(Res.string.auth_email_placeholder),
                errorMessage = state.emailError.toErrorMessage(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            AuthTextField(
                value = state.password,
                onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                label = stringResource(Res.string.auth_password),
                placeholder = stringResource(Res.string.auth_password_placeholder),
                errorMessage = state.passwordError.toErrorMessage(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (state.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                ),
                visualTransformation = PasswordVisualTransformation(),
            )
            AnimatedVisibility(
                visible = state.mode == AuthMode.REGISTER,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                AuthTextField(
                    value = state.username,
                    onValueChange = { onIntent(AuthIntent.UsernameChanged(it)) },
                    label = stringResource(Res.string.auth_username),
                    placeholder = stringResource(Res.string.auth_username_placeholder),
                    errorMessage = state.usernameError.toErrorMessage(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
            AuthPrimaryButton(
                text = submitLabel,
                loading = state.isSubmitting,
                enabled = !state.isBusy,
                onClick = { onIntent(AuthIntent.SubmitClicked) },
            )
            AuthDivider(text = stringResource(Res.string.auth_or))
            AuthGoogleButton(
                text = stringResource(Res.string.auth_google),
                loading = state.isGoogleSubmitting,
                enabled = !state.isBusy,
                onClick = { onIntent(AuthIntent.GoogleClicked) },
            )
        }
    }
}
