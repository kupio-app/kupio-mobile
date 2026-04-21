package kupio.mobile.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class ApiFieldError(
    val field: String?,
    val message: String,
    val type: String? = null,
    val context: Map<String, String> = emptyMap(),
)

class ApiException(
    val statusCode: Int,
    override val message: String,
    val fieldErrors: List<ApiFieldError> = emptyList(),
) : Exception(message)

private val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (status.value in 200..299) {
        return body()
    }
    throw toApiException()
}

suspend fun HttpResponse.toApiException(): ApiException {
    val rawBody = bodyAsText()
    val payload = rawBody
        .takeIf { it.isNotBlank() }
        ?.let { runCatching { ApiJson.parseToJsonElement(it) }.getOrNull() }

    val fieldErrors = payload.extractFieldErrors()
    val message = payload.extractMessage(fieldErrors)
        ?: "Request failed with status ${status.value}."

    return ApiException(
        statusCode = status.value,
        message = message,
        fieldErrors = fieldErrors,
    )
}

private fun JsonElement?.extractFieldErrors(): List<ApiFieldError> {
    val detail = (this as? JsonObject)?.get("detail") as? JsonArray ?: return emptyList()
    return detail.mapNotNull { item ->
        val errorObject = item as? JsonObject ?: return@mapNotNull null
        val field = errorObject.readFieldName()
        val message = errorObject["msg"]?.asString() ?: return@mapNotNull null
        ApiFieldError(
            field = field,
            message = message,
            type = errorObject["type"]?.asString(),
            context = errorObject["ctx"].readStringMap(),
        )
    }
}

private fun JsonElement?.extractMessage(
    fieldErrors: List<ApiFieldError>,
): String? {
    val detail = (this as? JsonObject)?.get("detail")
    return when (detail) {
        is JsonPrimitive -> detail.contentOrNull
        is JsonArray -> fieldErrors.firstOrNull()?.message
        else -> null
    }
}

private fun JsonObject.readFieldName(): String? {
    val location = this["loc"] as? JsonArray ?: return null
    return location
        .mapNotNull { it.asString() }
        .lastOrNull { it != "body" }
}

private fun JsonElement.asString(): String? {
    return (this as? JsonPrimitive)?.contentOrNull
}

private fun JsonElement?.readStringMap(): Map<String, String> {
    val jsonObject = this as? JsonObject ?: return emptyMap()
    return jsonObject.mapNotNull { (key, value) ->
        value.asString()?.let { stringValue -> key to stringValue }
    }.toMap()
}
