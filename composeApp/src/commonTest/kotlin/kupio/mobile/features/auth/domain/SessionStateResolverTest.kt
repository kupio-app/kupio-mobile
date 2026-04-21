package kupio.mobile.features.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.SessionStateResolver

class SessionStateResolverTest {
    private val resolver = SessionStateResolver()

    @Test
    fun `resolve returns signed in when username is complete`() {
        val user = sampleUser(needsUsername = false)
        assertEquals(SessionState.SignedIn(user), resolver.resolve(user))
    }

    @Test
    fun `resolve returns needs username when backend requires it`() {
        val user = sampleUser(needsUsername = true)
        assertEquals(SessionState.NeedsUsername(user), resolver.resolve(user))
    }

    private fun sampleUser(
        needsUsername: Boolean,
    ) = AuthenticatedUser(
        id = "user-1",
        username = if (needsUsername) null else "kupio_user",
        displayName = "Kupio User",
        email = "hello@kupio.dev",
        role = "user",
        needsUsername = needsUsername,
        balance = 0,
        avatarUrl = null,
    )
}
