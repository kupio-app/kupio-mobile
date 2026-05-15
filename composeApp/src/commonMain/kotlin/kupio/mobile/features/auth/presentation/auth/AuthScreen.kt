package kupio.mobile.features.auth.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.features.auth.presentation.auth.components.AuthPasswordField
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.auth.presentation.auth.components.AuthHero
import kupio.mobile.features.auth.presentation.auth.components.AuthHeadlineText
import kupio.mobile.features.auth.presentation.auth.components.AuthInlineError
import kupio.mobile.features.auth.presentation.auth.components.AuthDivider
import kupio.mobile.features.auth.presentation.auth.components.AuthGoogleButton
import kupio.mobile.features.auth.presentation.auth.components.AuthModeFooter
import kupio.mobile.features.auth.presentation.auth.components.PasswordStrengthMeter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import org.koin.compose.viewmodel.koinViewModel
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.features.auth.presentation.auth.components.AuthActionButton
import kupio.mobile.features.auth.presentation.auth.components.AuthTopNavbar
import kupio.mobile.features.auth.presentation.auth.components.AuthViewport
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_confirm_password
import mobile.composeapp.generated.resources.auth_confirm_password_placeholder
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_create_account
import mobile.composeapp.generated.resources.auth_email
import mobile.composeapp.generated.resources.auth_email_placeholder
import mobile.composeapp.generated.resources.auth_forgot_password
import mobile.composeapp.generated.resources.auth_login
import mobile.composeapp.generated.resources.auth_password
import mobile.composeapp.generated.resources.auth_password_placeholder
import mobile.composeapp.generated.resources.auth_username
import mobile.composeapp.generated.resources.auth_username_placeholder
import org.jetbrains.compose.resources.stringResource

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

                            override fun onFailure(errorCode: String) {
                                viewModel.onIntent(AuthIntent.GoogleFailure(errorCode))
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
    Scaffold(
        topBar = {
            AuthTopNavbar(onBackClick = {})
        },
        bottomBar = {
            AuthModeFooter(currentMode = state.mode, onSwitchMode = { onIntent(AuthIntent.ModeSelected(it)) })
        }
    ) { paddingValues ->
        AuthViewport(
            modifier = Modifier
                .padding(paddingValues)
                .testTag("auth.screen"),
        ) {
            AuthHero()

            Spacer(modifier = Modifier.height(18.dp))

            if (state.mode == AuthMode.LOGIN) {
                AuthHeadlineText(text = stringResource(Res.string.auth_login))
            } else {
                AuthHeadlineText(text = stringResource(Res.string.auth_create_account))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthInlineError(message = state.formError?.toErrorMessage())

            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                KupioTextField(
                    value = state.email,
                    onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
                    label = stringResource(Res.string.auth_email),
                    placeholder = stringResource(Res.string.auth_email_placeholder),
                    inputModifier = Modifier.testTag("auth.email"),
                    errorModifier = Modifier.testTag("auth.email-error"),
                    error = state.emailError?.toErrorMessage(),
                    required = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    )
                )

                AuthPasswordField(
                    value = state.password,
                    onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
                    label = stringResource(Res.string.auth_password),
                    placeholder = stringResource(Res.string.auth_password_placeholder),
                    inputModifier = Modifier.testTag("auth.password"),
                    errorModifier = Modifier.testTag("auth.password-error"),
                    error = state.passwordError?.toErrorMessage(),
                    required = true,
                    trailingLabel = "8+ chars",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                    )
                )

                AnimatedVisibility(
                    visible = state.mode == AuthMode.REGISTER,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PasswordStrengthMeter(password = state.password)

                        AuthPasswordField(
                            value = state.confirmPassword,
                            onValueChange = { onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                            label = stringResource(Res.string.auth_confirm_password),
                            placeholder = stringResource(Res.string.auth_confirm_password_placeholder),
                            inputModifier = Modifier.testTag("auth.confirm-password"),
                            errorModifier = Modifier.testTag("auth.confirm-password-error"),
                            error = state.confirmPasswordError?.toErrorMessage(),
                            required = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            )
                        )

                        KupioTextField(
                            value = state.username,
                            onValueChange = { onIntent(AuthIntent.UsernameChanged(it)) },
                            label = stringResource(Res.string.auth_username),
                            placeholder = stringResource(Res.string.auth_username_placeholder),
                            inputModifier = Modifier.testTag("auth.username"),
                            errorModifier = Modifier.testTag("auth.username-error"),
                            error = state.usernameError?.toErrorMessage(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                            )
                        )
                    }
                }

                if (state.mode == AuthMode.LOGIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(Res.string.auth_forgot_password),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { /* no op for now */ }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthActionButton(
                text = if (state.mode == AuthMode.LOGIN) stringResource(Res.string.auth_login) else stringResource(Res.string.auth_continue),
                onClick = { onIntent(AuthIntent.SubmitClicked) },
                modifier = Modifier.testTag("auth.submit"),
                enabled = !state.isBusy,
                loading = state.isSubmitting,
                trailingIcon = Icons.Outlined.ChevronRight,
            )

            AuthDivider()

            AuthGoogleButton(
                loading = state.isGoogleSubmitting,
                onClick = { onIntent(AuthIntent.GoogleClicked) }
            )
        }
    }
}
