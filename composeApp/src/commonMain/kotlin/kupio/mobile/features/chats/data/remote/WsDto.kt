package kupio.mobile.features.chats.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class WsAuthRequestDto(val type: String = "auth", val token: String)

@Serializable
data class WsPingRequestDto(val type: String = "ping")

@Serializable
data class WsTypingRequestDto(val type: String = "typing")

internal sealed interface WsServerEvent {
    data object AuthOk : WsServerEvent
    data object Pong : WsServerEvent
    data class NewMessage(val dto: MessageResponseDto) : WsServerEvent
    data class MessageDeleted(val messageId: String) : WsServerEvent
    data class Typing(val userId: String) : WsServerEvent
    data class Error(val code: String) : WsServerEvent
    data object Unknown : WsServerEvent
}

private val wsJson = Json { ignoreUnknownKeys = true }

internal fun parseWsFrame(text: String): WsServerEvent = try {
    val obj = wsJson.parseToJsonElement(text).jsonObject
    when (obj["type"]?.jsonPrimitive?.contentOrNull) {
        "auth_ok" -> WsServerEvent.AuthOk
        "pong" -> WsServerEvent.Pong
        "new_message" -> WsServerEvent.NewMessage(wsJson.decodeFromJsonElement(obj))
        "message_deleted" -> WsServerEvent.MessageDeleted(
            obj["message_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        )
        "typing" -> WsServerEvent.Typing(
            obj["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
        )
        "error" -> WsServerEvent.Error(
            obj["code"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        )
        null -> if (obj.containsKey("sender_id")) {
            WsServerEvent.NewMessage(wsJson.decodeFromJsonElement(obj))
        } else {
            WsServerEvent.Unknown
        }
        else -> WsServerEvent.Unknown
    }
} catch (_: Exception) {
    WsServerEvent.Unknown
}
