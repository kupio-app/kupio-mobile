package kupio.mobile.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kupio.mobile.debug.TestAuthContract.AccessToken
import kupio.mobile.debug.TestAuthContract.ClearAuthAction
import kupio.mobile.debug.TestAuthContract.ExtraEmail
import kupio.mobile.debug.TestAuthContract.ExtraUserId
import kupio.mobile.debug.TestAuthContract.ExtraUsername
import kupio.mobile.debug.TestAuthContract.RefreshToken
import kupio.mobile.debug.TestAuthContract.SeedAuthAction
import kupio.mobile.debug.TestAuthContract.TokenLifetimeSeconds
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.UserRole
import kupio.mobile.features.auth.domain.session.CachedAuthenticatedUserStore
import kupio.mobile.features.auth.domain.session.SecureSessionStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TestAuthReceiver : BroadcastReceiver(), KoinComponent {
    private val secureSessionStore: SecureSessionStore by inject()
    private val cachedAuthenticatedUserStore: CachedAuthenticatedUserStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(SeedAuthAction, ClearAuthAction)) return

        if (intent.action == ClearAuthAction) {
            runBlocking(Dispatchers.IO) {
                secureSessionStore.clear()
                cachedAuthenticatedUserStore.clear()
            }
            return
        }

        val userId = intent.getStringExtra(ExtraUserId).orEmpty().ifBlank { "appium-user" }
        val email = intent.getStringExtra(ExtraEmail).orEmpty().ifBlank { "appium@example.com" }
        val username = intent.getStringExtra(ExtraUsername).orEmpty().ifBlank { "appium" }
        val nowSeconds = System.currentTimeMillis() / 1000

        runBlocking(Dispatchers.IO) {
            secureSessionStore.writeSession(
                AuthSession(
                    accessToken = AccessToken,
                    refreshToken = RefreshToken,
                    accessExpiresAt = nowSeconds + TokenLifetimeSeconds,
                    refreshExpiresAt = nowSeconds + TokenLifetimeSeconds,
                )
            )
            cachedAuthenticatedUserStore.write(
                AuthenticatedUser(
                    id = userId,
                    username = username,
                    displayName = "Appium User",
                    email = email,
                    role = UserRole.USER,
                    needsUsername = false,
                    balance = 0,
                    avatarUrl = null,
                )
            )
        }
    }
}
