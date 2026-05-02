package kupio.mobile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kupio.mobile.core.analytics.AnalyticsService

fun createKupioHttpClient(
    baseUrl: String,
    isDebug: Boolean,
    analyticsService: AnalyticsService,
): HttpClient {
    return HttpClient {
        expectSuccess = false

        install(WebSockets)

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }

        if (isDebug) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) = println("[HTTP] $message")
                }
                level = LogLevel.INFO
            }
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                analyticsService.log("API error: ${request.url} -> ${exception.message}")
                analyticsService.recordException(
                    exception,
                    mapOf("url" to request.url.toString())
                )
            }
        }
    }
}
