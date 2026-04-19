package kupio.mobile.features.auth.domain

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
