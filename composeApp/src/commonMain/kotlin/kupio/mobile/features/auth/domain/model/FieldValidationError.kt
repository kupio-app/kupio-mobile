package kupio.mobile.features.auth.domain.model

sealed interface FieldValidationError {
    data object Required : FieldValidationError
    data object InvalidEmail : FieldValidationError
    data object PasswordTooShort : FieldValidationError
    data object PasswordsDoNotMatch : FieldValidationError
    data object UsernameTooShort : FieldValidationError
    data object UsernameTooLong : FieldValidationError
}
