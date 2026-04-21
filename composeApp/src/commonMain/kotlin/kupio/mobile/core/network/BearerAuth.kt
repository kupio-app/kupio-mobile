package kupio.mobile.core.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

fun HttpRequestBuilder.bearerAuth(
    accessToken: String,
) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
}
