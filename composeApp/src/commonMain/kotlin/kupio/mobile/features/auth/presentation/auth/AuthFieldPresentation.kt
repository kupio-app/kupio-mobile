package kupio.mobile.features.auth.presentation.auth

import androidx.compose.runtime.Composable
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.model.FieldValidationError
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_error_email_invalid
import mobile.composeapp.generated.resources.auth_error_password_short
import mobile.composeapp.generated.resources.auth_error_required
import mobile.composeapp.generated.resources.auth_error_username_long
import mobile.composeapp.generated.resources.auth_error_username_short
import org.jetbrains.compose.resources.stringResource

internal data class AuthFieldErrors(
    val emailError: FieldValidationError? = null,
    val passwordError: FieldValidationError? = null,
    val usernameError: FieldValidationError? = null,
) {
    val hasAny: Boolean
        get() = emailError != null || passwordError != null || usernameError != null
}

internal fun ApiException.toAuthFieldErrors(): AuthFieldErrors {
    var emailError: FieldValidationError? = null
    var passwordError: FieldValidationError? = null
    var usernameError: FieldValidationError? = null

    fieldErrors.forEach { error ->
        when (error.field) {
            "email" -> emailError = mapFieldError(error.message, AuthField.EMAIL)
            "password" -> passwordError = mapFieldError(error.message, AuthField.PASSWORD)
            "username" -> usernameError = mapFieldError(error.message, AuthField.USERNAME)
        }
    }

    return AuthFieldErrors(
        emailError = emailError,
        passwordError = passwordError,
        usernameError = usernameError,
    )
}

internal fun mapFieldError(
    message: String,
    field: AuthField,
): FieldValidationError? {
    val normalized = message.lowercase()
    return when (field) {
        AuthField.EMAIL -> when {
            "required" in normalized -> FieldValidationError.Required
            "email" in normalized -> FieldValidationError.InvalidEmail
            else -> null
        }

        AuthField.PASSWORD -> when {
            "required" in normalized -> FieldValidationError.Required
            "at least 8" in normalized || "min" in normalized -> FieldValidationError.PasswordTooShort
            else -> null
        }

        AuthField.USERNAME -> when {
            "required" in normalized -> FieldValidationError.Required
            "at least 3" in normalized || "too short" in normalized -> FieldValidationError.UsernameTooShort
            "at most 50" in normalized || "too long" in normalized -> FieldValidationError.UsernameTooLong
            else -> null
        }
    }
}

internal fun Throwable.toUserMessage(): String {
    return message ?: "Something went wrong. Please try again."
}

@Composable
internal fun FieldValidationError?.toErrorMessage(): String? {
    return when (this) {
        FieldValidationError.InvalidEmail -> stringResource(Res.string.auth_error_email_invalid)
        FieldValidationError.PasswordTooShort -> stringResource(Res.string.auth_error_password_short)
        FieldValidationError.Required -> stringResource(Res.string.auth_error_required)
        FieldValidationError.UsernameTooLong -> stringResource(Res.string.auth_error_username_long)
        FieldValidationError.UsernameTooShort -> stringResource(Res.string.auth_error_username_short)
        null -> null
    }
}

internal enum class AuthField {
    EMAIL,
    PASSWORD,
    USERNAME,
}
