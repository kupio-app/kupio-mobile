package kupio.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.auth.domain.FieldValidationError
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_continue
import mobile.composeapp.generated.resources.auth_error_required
import mobile.composeapp.generated.resources.auth_error_username_long
import mobile.composeapp.generated.resources.auth_error_username_short
import mobile.composeapp.generated.resources.auth_one_last_step
import mobile.composeapp.generated.resources.auth_username
import mobile.composeapp.generated.resources.auth_username_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class UsernameScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<UsernameViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        UsernameRoute(
            state = state,
            onAction = viewModel::onAction,
        )
    }
}

@Composable
private fun UsernameRoute(
    state: UsernameState,
    onAction: (UsernameAction) -> Unit,
) {
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
                text = stringResource(Res.string.auth_one_last_step),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (state.email.isNotBlank()) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(Res.string.auth_username_hint),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.formError != null) {
                Text(
                    text = state.formError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedTextField(
                value = state.username,
                onValueChange = { onAction(UsernameAction.UsernameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.auth_username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                isError = state.usernameError != null,
                supportingText = {
                    val errorText = state.usernameError.toUsernameErrorMessage()
                    if (errorText != null) {
                        Text(errorText)
                    }
                },
            )
            Button(
                onClick = { onAction(UsernameAction.SubmitClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = stringResource(Res.string.auth_continue))
                }
            }
        }
    }
}

@Composable
private fun FieldValidationError?.toUsernameErrorMessage(): String? {
    return when (this) {
        FieldValidationError.Required -> stringResource(Res.string.auth_error_required)
        FieldValidationError.UsernameTooLong -> stringResource(Res.string.auth_error_username_long)
        FieldValidationError.UsernameTooShort -> stringResource(Res.string.auth_error_username_short)
        else -> null
    }
}
