package kupio.mobile.features.auth.domain.session

import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState

fun AuthenticatedUser.toSessionState(): SessionState =
    if (needsUsername) SessionState.NeedsUsername(this) else SessionState.SignedIn(this)
