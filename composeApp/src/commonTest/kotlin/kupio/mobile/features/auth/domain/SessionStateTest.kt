package kupio.mobile.features.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.model.toSessionState

class SessionStateTest {
    @Test
    fun `toSessionState returns signed in when username is complete`() {
        val user = sampleUser(needsUsername = false)
        assertEquals(SessionState.SignedIn(user), user.toSessionState())
    }

    @Test
    fun `toSessionState returns needs username when backend requires it`() {
        val user = sampleUser(needsUsername = true)
        assertEquals(SessionState.NeedsUsername(user), user.toSessionState())
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
