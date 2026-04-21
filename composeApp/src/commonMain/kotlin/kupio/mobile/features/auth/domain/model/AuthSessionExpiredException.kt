package kupio.mobile.features.auth.domain.model

class AuthSessionExpiredException(
    message: String = "Your session has expired. Please sign in again.",
) : Exception(message)
