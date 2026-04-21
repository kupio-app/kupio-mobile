package kupio.mobile.features.auth.presentation.auth

import androidx.compose.runtime.Composable
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.model.FieldValidationError
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_error_email_invalid
import mobile.composeapp.generated.resources.auth_error_generic
import mobile.composeapp.generated.resources.auth_error_invalid_credentials
import mobile.composeapp.generated.resources.auth_error_password_short
import mobile.composeapp.generated.resources.auth_error_passwords_do_not_match
import mobile.composeapp.generated.resources.auth_error_required
import mobile.composeapp.generated.resources.auth_error_session_expired
import mobile.composeapp.generated.resources.auth_error_username_long
import mobile.composeapp.generated.resources.auth_error_username_short
import mobile.composeapp.generated.resources.auth_google_error_activity_unavailable
import mobile.composeapp.generated.resources.auth_google_error_failed
import mobile.composeapp.generated.resources.auth_google_error_invalid_response
import mobile.composeapp.generated.resources.auth_google_error_missing_id_token
import mobile.composeapp.generated.resources.auth_google_error_no_credential
import mobile.composeapp.generated.resources.auth_google_error_not_configured
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

internal fun Throwable.toAuthFormError(
    useInvalidCredentials: Boolean = true,
): AuthFormError {
    return when {
        this is AuthSessionExpiredException -> AuthFormError.SessionExpired
        this is ApiException && statusCode == 401 && useInvalidCredentials -> AuthFormError.InvalidCredentials
        else -> AuthFormError.Generic
    }
}

@Composable
internal fun FieldValidationError?.toErrorMessage(): String? {
    return when (this) {
        FieldValidationError.InvalidEmail -> stringResource(Res.string.auth_error_email_invalid)
        FieldValidationError.PasswordTooShort -> stringResource(Res.string.auth_error_password_short)
        FieldValidationError.PasswordsDoNotMatch -> stringResource(Res.string.auth_error_passwords_do_not_match)
        FieldValidationError.Required -> stringResource(Res.string.auth_error_required)
        FieldValidationError.UsernameTooLong -> stringResource(Res.string.auth_error_username_long)
        FieldValidationError.UsernameTooShort -> stringResource(Res.string.auth_error_username_short)
        null -> null
    }
}

@Composable
internal fun AuthFormError?.toErrorMessage(): String? {
    return when (this) {
        AuthFormError.Generic -> stringResource(Res.string.auth_error_generic)
        AuthFormError.InvalidCredentials -> stringResource(Res.string.auth_error_invalid_credentials)
        AuthFormError.SessionExpired -> stringResource(Res.string.auth_error_session_expired)
        is AuthFormError.GoogleSignIn -> googleSignInErrorMessage(errorCode)
        null -> null
    }
}

@Composable
private fun googleSignInErrorMessage(
    errorCode: String,
): String {
    return when (errorCode) {
        GoogleSignInFailureCode.NotConfigured -> stringResource(Res.string.auth_google_error_not_configured)
        GoogleSignInFailureCode.ActivityUnavailable -> stringResource(Res.string.auth_google_error_activity_unavailable)
        GoogleSignInFailureCode.MissingIdToken -> stringResource(Res.string.auth_google_error_missing_id_token)
        GoogleSignInFailureCode.NoCredential -> stringResource(Res.string.auth_google_error_no_credential)
        GoogleSignInFailureCode.InvalidResponse -> stringResource(Res.string.auth_google_error_invalid_response)
        else -> stringResource(Res.string.auth_google_error_failed)
    }
}

internal enum class AuthField {
    EMAIL,
    PASSWORD,
    USERNAME,
}
