package kupio.mobile.core.notifications.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kupio.mobile.core.config.getPlatformName
import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.core.notifications.NotificationsRepository

class NotificationsRepositoryImpl(
    private val authenticatedApiClient: AuthenticatedApiClient,
    private val notifApi: NotificationsApi
) : NotificationsRepository {

    override suspend fun sendPushToken(token: String) {
        val currentPlatform = getPlatformName()

        val request = CreatePushTokenRequestDto(
            token = token,
            platform = currentPlatform
        )

        authenticatedApiClient.request { authorize ->
            notifApi.sendPushToken(
                authorize = authorize,
                request = request,
            )
        }
    }
}