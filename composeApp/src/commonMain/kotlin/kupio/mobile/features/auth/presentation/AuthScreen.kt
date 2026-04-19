package kupio.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.auth.domain.FieldValidationError
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_create_account
import mobile.composeapp.generated.resources.auth_email
import mobile.composeapp.generated.resources.auth_error_email_invalid
import mobile.composeapp.generated.resources.auth_error_password_short
import mobile.composeapp.generated.resources.auth_error_required
import mobile.composeapp.generated.resources.auth_error_username_long
import mobile.composeapp.generated.resources.auth_error_username_short
import mobile.composeapp.generated.resources.auth_google
import mobile.composeapp.generated.resources.auth_loading
import mobile.composeapp.generated.resources.auth_login
import mobile.composeapp.generated.resources.auth_or
import mobile.composeapp.generated.resources.auth_password
import mobile.composeapp.generated.resources.auth_register_subtitle
import mobile.composeapp.generated.resources.auth_sign_in_subtitle
import mobile.composeapp.generated.resources.auth_username
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
                                viewModel.onAction(AuthAction.GoogleSuccess(idToken))
                            }

                            override fun onFailure(message: String) {
                                viewModel.onAction(AuthAction.GoogleFailure(message))
                            }

                            override fun onCancelled() {
                                viewModel.onAction(AuthAction.GoogleCancelled)
                            }
                        },
                    )
                }
            }
        }

        AuthRoute(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun AuthRoute(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
) {
    val subtitle = when (state.mode) {
        AuthMode.LOGIN -> stringResource(Res.string.auth_sign_in_subtitle)
        AuthMode.REGISTER -> stringResource(Res.string.auth_register_subtitle)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(KupioThemeDefaults.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            Text(
                text = "Kupio",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AuthModeSwitcher(
                selectedMode = state.mode,
                onModeSelected = { onAction(AuthAction.ModeSelected(it)) },
            )
            if (state.formError != null) {
                Text(
                    text = state.formError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedTextField(
                value = state.email,
                onValueChange = { onAction(AuthAction.EmailChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.auth_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                isError = state.emailError != null,
                supportingText = {
                    val errorText = state.emailError.toErrorMessage()
                    if (errorText != null) {
                        Text(errorText)
                    }
                },
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.auth_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (state.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                ),
                isError = state.passwordError != null,
                supportingText = {
                    val errorText = state.passwordError.toErrorMessage()
                    if (errorText != null) {
                        Text(errorText)
                    }
                },
            )
            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onAction(AuthAction.UsernameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.auth_username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    isError = state.usernameError != null,
                    supportingText = {
                        val errorText = state.usernameError.toErrorMessage()
                        if (errorText != null) {
                            Text(errorText)
                        }
                    },
                )
            }
            Button(
                onClick = { onAction(AuthAction.SubmitClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && !state.isGoogleSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = when (state.mode) {
                            AuthMode.LOGIN -> stringResource(Res.string.auth_continue)
                            AuthMode.REGISTER -> stringResource(Res.string.auth_create_account)
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.auth_or),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = { onAction(AuthAction.GoogleClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting && !state.isGoogleSubmitting,
            ) {
                if (state.isGoogleSubmitting) {
                    CircularProgressIndicator()
                } else {
                    Text(text = stringResource(Res.string.auth_google))
                }
            }
            Text(
                text = stringResource(Res.string.auth_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthModeSwitcher(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        AuthModeButton(
            text = stringResource(Res.string.auth_login),
            isSelected = selectedMode == AuthMode.LOGIN,
            onClick = { onModeSelected(AuthMode.LOGIN) },
            modifier = Modifier.weight(1f),
        )
        AuthModeButton(
            text = stringResource(Res.string.auth_create_account),
            isSelected = selectedMode == AuthMode.REGISTER,
            onClick = { onModeSelected(AuthMode.REGISTER) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(text = text)
        }
    }
}

@Composable
private fun FieldValidationError?.toErrorMessage(): String? {
    return when (this) {
        FieldValidationError.InvalidEmail -> stringResource(Res.string.auth_error_email_invalid)
        FieldValidationError.PasswordTooShort -> stringResource(Res.string.auth_error_password_short)
        FieldValidationError.Required -> stringResource(Res.string.auth_error_required)
        FieldValidationError.UsernameTooLong -> stringResource(Res.string.auth_error_username_long)
        FieldValidationError.UsernameTooShort -> stringResource(Res.string.auth_error_username_short)
        null -> null
    }
}
