package kupio.mobile.features.chats.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kupio.mobile.core.config.BackendConfig
import kupio.mobile.features.auth.data.repository.AuthTokenProvider
import kupio.mobile.features.auth.domain.model.AuthSessionExpiredException
import kupio.mobile.features.auth.domain.model.SessionState
import kupio.mobile.features.auth.domain.session.AuthSessionManager
import kupio.mobile.features.chats.data.remote.WsAuthRequestDto
import kupio.mobile.features.chats.data.remote.WsServerEvent
import kupio.mobile.features.chats.data.remote.WsTypingRequestDto
import kupio.mobile.features.chats.data.remote.parseWsFrame
import kupio.mobile.features.chats.domain.model.WsMessageEvent
import kotlin.time.Duration.Companion.milliseconds

private const val RECONNECT_DELAY_MS = 3_000L

class ChatWebSocket(
    private val httpClient: HttpClient,
    private val config: BackendConfig,
    private val authTokenProvider: AuthTokenProvider,
    private val sessionManager: AuthSessionManager,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val outgoing = HashMap<String, Channel<String>>()
    private val outgoingMutex = Mutex()

    fun observe(conversationId: String): Flow<WsMessageEvent> = channelFlow {
        val outgoingCh = Channel<String>(Channel.BUFFERED)
        outgoingMutex.withLock { outgoing[conversationId] = outgoingCh }
        try {
            var lastMessageId: String? = null
            var stop = false

            while (!stop) {
                try {
                    val token = authTokenProvider.freshAccessToken()
                    val wsUrl = buildWsUrl(conversationId, lastMessageId)
                    val currentUserId = currentUserId()

                    httpClient.webSocket(wsUrl) {
                        send(json.encodeToString(WsAuthRequestDto(token = token)))

                        var authenticated = false

                        val sender = launch {
                            for (frame in outgoingCh) {
                                runCatching { send(frame) }
                            }
                        }

                        try {
                            loop@ for (frame in incoming) {
                                when (frame) {
                                    is Frame.Close -> break@loop
                                    is Frame.Text -> when (val event = parseWsFrame(frame.readText())) {
                                        WsServerEvent.AuthOk -> authenticated = true
                                        is WsServerEvent.NewMessage -> if (authenticated) {
                                            lastMessageId = event.dto.id
                                            this@channelFlow.send(
                                                WsMessageEvent.Received(event.dto.toItem(currentUserId))
                                            )
                                        }
                                        is WsServerEvent.MessageDeleted -> if (authenticated) {
                                            this@channelFlow.send(WsMessageEvent.Deleted(event.messageId))
                                        }
                                        is WsServerEvent.Typing -> if (authenticated && event.userId != currentUserId) {
                                            this@channelFlow.send(WsMessageEvent.TypingStarted)
                                        }
                                        is WsServerEvent.Error -> {
                                            if (event.code == "forbidden") stop = true
                                            break@loop
                                        }
                                        else -> {}
                                    }
                                    else -> {}
                                }
                            }
                        } finally {
                            sender.cancel()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AuthSessionExpiredException) {
                    sessionManager.expireSession()
                    stop = true
                } catch (_: Exception) {
                    // network drop or transient error - reconnect
                }

                if (!stop) delay(RECONNECT_DELAY_MS.milliseconds)
            }
        } finally {
            outgoingMutex.withLock { outgoing.remove(conversationId) }
            outgoingCh.close()
        }
    }

    suspend fun sendTyping(conversationId: String) {
        val frame = json.encodeToString(WsTypingRequestDto())
        outgoingMutex.withLock { outgoing[conversationId] }?.trySend(frame)
    }

    private fun buildWsUrl(conversationId: String, lastMessageId: String?): String {
        val wsBase = config.baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val path = "$wsBase/api/chat/conversations/$conversationId/ws"
        return if (lastMessageId != null) "$path?last_message_id=$lastMessageId" else path
    }

    private fun currentUserId() =
        (sessionManager.sessionState.value as? SessionState.SignedIn)?.user?.id.orEmpty()
}
