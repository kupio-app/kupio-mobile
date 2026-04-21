package kupio.mobile.features.auth.data.local

import com.liftric.kvault.KVault
import kupio.mobile.features.auth.domain.model.AuthSession
import kupio.mobile.features.auth.domain.session.SecureSessionStore

private const val AccessTokenKey = "auth.access_token"
private const val RefreshTokenKey = "auth.refresh_token"
private const val AccessExpiresAtKey = "auth.access_expires_at"
private const val RefreshExpiresAtKey = "auth.refresh_expires_at"
private const val TokenTypeKey = "auth.token_type"
private const val NeedsUsernameKey = "auth.needs_username"

class KVaultSecureSessionStore(
    private val vault: KVault,
) : SecureSessionStore {
    override suspend fun readSession(): AuthSession? {
        val accessToken = vault.string(forKey = AccessTokenKey)
        val refreshToken = vault.string(forKey = RefreshTokenKey)
        val accessExpiresAt = vault.string(forKey = AccessExpiresAtKey)?.toLongOrNull()
        val refreshExpiresAt = vault.string(forKey = RefreshExpiresAtKey)?.toLongOrNull()

        if (
            accessToken == null ||
            refreshToken == null ||
            accessExpiresAt == null ||
            refreshExpiresAt == null
        ) {
            return null
        }

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessExpiresAt = accessExpiresAt,
            refreshExpiresAt = refreshExpiresAt,
            tokenType = vault.string(forKey = TokenTypeKey).orEmpty().ifBlank { "bearer" },
            needsUsername = vault.bool(forKey = NeedsUsernameKey) ?: false,
        )
    }

    override suspend fun writeSession(
        session: AuthSession,
    ) {
        vault.set(key = AccessTokenKey, stringValue = session.accessToken)
        vault.set(key = RefreshTokenKey, stringValue = session.refreshToken)
        vault.set(key = AccessExpiresAtKey, stringValue = session.accessExpiresAt.toString())
        vault.set(key = RefreshExpiresAtKey, stringValue = session.refreshExpiresAt.toString())
        vault.set(key = TokenTypeKey, stringValue = session.tokenType)
        vault.set(key = NeedsUsernameKey, boolValue = session.needsUsername)
    }

    override suspend fun clear() {
        vault.clear()
    }
}
