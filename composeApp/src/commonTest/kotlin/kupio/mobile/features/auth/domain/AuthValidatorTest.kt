package kupio.mobile.features.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kupio.mobile.features.auth.domain.model.FieldValidationError
import kupio.mobile.features.auth.domain.validation.AuthValidator

class AuthValidatorTest {
    private val validator = AuthValidator()

    @Test
    fun `email validation rejects blank and malformed values`() {
        assertEquals(FieldValidationError.Required, validator.validateEmail(""))
        assertEquals(FieldValidationError.InvalidEmail, validator.validateEmail("kupio"))
        assertEquals(null, validator.validateEmail("hello@kupio.dev"))
    }

    @Test
    fun `password validation enforces api minimum length`() {
        assertEquals(FieldValidationError.Required, validator.validatePassword(""))
        assertEquals(FieldValidationError.PasswordTooShort, validator.validatePassword("short"))
        assertEquals(null, validator.validatePassword("password123"))
    }

    @Test
    fun `confirm password validation requires matching password`() {
        assertEquals(
            FieldValidationError.Required,
            validator.validateConfirmPassword(password = "password123", confirmPassword = ""),
        )
        assertEquals(
            FieldValidationError.PasswordsDoNotMatch,
            validator.validateConfirmPassword(password = "password123", confirmPassword = "password124"),
        )
        assertEquals(
            null,
            validator.validateConfirmPassword(password = "password123", confirmPassword = "password123"),
        )
    }

    @Test
    fun `username validation follows openapi length bounds`() {
        assertEquals(FieldValidationError.Required, validator.validateUsername(""))
        assertEquals(FieldValidationError.UsernameTooShort, validator.validateUsername("ab"))
        assertEquals(FieldValidationError.UsernameTooLong, validator.validateUsername("a".repeat(51)))
        assertEquals(null, validator.validateUsername("kupio_user"))
    }
}
