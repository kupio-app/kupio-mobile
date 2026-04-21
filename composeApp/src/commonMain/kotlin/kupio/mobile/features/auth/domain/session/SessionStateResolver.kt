package kupio.mobile.features.auth.domain.session

import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState

class SessionStateResolver {
    fun resolve(
        user: AuthenticatedUser,
    ): SessionState {
        return if (user.needsUsername) {
            SessionState.NeedsUsername(user)
        } else {
            SessionState.SignedIn(user)
        }
    }
}
