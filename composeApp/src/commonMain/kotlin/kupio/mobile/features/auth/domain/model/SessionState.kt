package kupio.mobile.features.auth.domain.model

sealed interface SessionState {
    data object Loading : SessionState
    data object BootstrapFailed : SessionState
    data object SignedOut : SessionState
    data class NeedsUsername(val user: AuthenticatedUser) : SessionState
    data class SignedIn(val user: AuthenticatedUser) : SessionState
}

fun AuthenticatedUser.toSessionState(): SessionState =
    if (needsUsername) SessionState.NeedsUsername(this) else SessionState.SignedIn(this)
