package kupio.mobile.features.auth.domain.session

import kupio.mobile.features.auth.domain.model.AuthenticatedUser

interface CachedAuthenticatedUserStore {
    suspend fun read(): AuthenticatedUser?
    suspend fun write(user: AuthenticatedUser)
    suspend fun clear()
}
