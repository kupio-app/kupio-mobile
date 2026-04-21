package kupio.mobile.features.auth.domain.validation

import kupio.mobile.features.auth.domain.model.FieldValidationError

class AuthValidator {
    fun validateEmail(
        rawValue: String,
    ): FieldValidationError? {
        val value = rawValue.trim()
        return when {
            value.isEmpty() -> FieldValidationError.Required
            !EmailRegex.matches(value) -> FieldValidationError.InvalidEmail
            else -> null
        }
    }

    fun validatePassword(
        rawValue: String,
    ): FieldValidationError? {
        return if (rawValue.isBlank()) {
            FieldValidationError.Required
        } else if (rawValue.length < MinPasswordLength) {
            FieldValidationError.PasswordTooShort
        } else {
            null
        }
    }

    fun validateConfirmPassword(
        password: String,
        confirmPassword: String,
    ): FieldValidationError? {
        return when {
            confirmPassword.isBlank() -> FieldValidationError.Required
            confirmPassword != password -> FieldValidationError.PasswordsDoNotMatch
            else -> null
        }
    }

    fun validateUsername(
        rawValue: String,
    ): FieldValidationError? {
        val value = rawValue.trim()
        return when {
            value.isEmpty() -> FieldValidationError.Required
            value.length < MinUsernameLength -> FieldValidationError.UsernameTooShort
            value.length > MaxUsernameLength -> FieldValidationError.UsernameTooLong
            else -> null
        }
    }

    companion object {
        private const val MinPasswordLength = 8
        private const val MinUsernameLength = 3
        private const val MaxUsernameLength = 50
        private val EmailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
