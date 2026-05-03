package kupio.mobile.features.auth.data.local

import kupio.mobile.core.offline.db.CachedAuthenticatedUserEntity
import kupio.mobile.core.offline.db.KupioDatabase
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.auth.domain.model.UserRole
import kupio.mobile.features.auth.domain.session.CachedAuthenticatedUserStore

class RoomCachedAuthenticatedUserStore(
    database: KupioDatabase,
) : CachedAuthenticatedUserStore {
    private val dao = database.authenticatedUserDao()

    override suspend fun read(): AuthenticatedUser? =
        dao.get()?.toDomain()

    override suspend fun write(user: AuthenticatedUser) {
        dao.clear()
        dao.upsert(user.toEntity())
    }

    override suspend fun clear() {
        dao.clear()
    }
}

private fun CachedAuthenticatedUserEntity.toDomain(): AuthenticatedUser? {
    val parsedRole = runCatching { UserRole.valueOf(role) }.getOrNull() ?: return null
    return AuthenticatedUser(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
        role = parsedRole,
        needsUsername = needsUsername,
        balance = balance,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
    )
}

private fun AuthenticatedUser.toEntity(): CachedAuthenticatedUserEntity =
    CachedAuthenticatedUserEntity(
        id = id,
        username = username,
        displayName = displayName,
        email = email,
        role = role.name,
        needsUsername = needsUsername,
        balance = balance,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
    )
