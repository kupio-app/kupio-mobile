package kupio.mobile.features.listings.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kupio.mobile.core.network.bodyOrThrow
import kupio.mobile.features.listings.domain.model.ListingImageUpload

class ListingsApi(private val httpClient: HttpClient) {

    suspend fun getListing(
        authorize: HttpRequestBuilder.() -> Unit,
        id: String,
        countSeen: Boolean = false,
    ): ListingResponseDto = httpClient.get("/api/listings/$id") {
        authorize()
        url {
            parameters.append("count_seen", countSeen.toString())
        }
    }.bodyOrThrow()

    suspend fun getListings(
        query: String? = null,
        categoryId: Int? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null,
        isFree: Boolean? = null,
        isTradable: Boolean? = null,
        filters: String? = null,
        limit: Int = 20,
        cursor: String? = null,
    ): ListListingsResponseDto = httpClient.get("/api/listings") {
        url {
            query?.let { parameters.append("q", it) }
            categoryId?.let { parameters.append("category_id", it.toString()) }
            minPrice?.let { parameters.append("min_price", it.toString()) }
            maxPrice?.let { parameters.append("max_price", it.toString()) }
            isFree?.let { parameters.append("is_free", it.toString()) }
            isTradable?.let { parameters.append("is_tradable", it.toString()) }
            filters?.let { parameters.append("filters", it) }
            parameters.append("limit", limit.toString())
            cursor?.let { parameters.append("cursor", it) }
        }
    }.bodyOrThrow()

    suspend fun createListing(
        authorize: HttpRequestBuilder.() -> Unit,
        request: ListingRequestDto,
    ): ListingResponseDto = httpClient.post("/api/listings") {
        authorize()
        setBody(request)
    }.bodyOrThrow()

    suspend fun updateListing(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        request: ListingRequestDto,
    ): ListingResponseDto = httpClient.put("/api/listings/$listingId") {
        authorize()
        setBody(request)
    }.bodyOrThrow()

    suspend fun updateListingStatus(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        request: ListingStatusUpdateRequestDto,
    ): ListingResponseDto = httpClient.put("/api/listings/$listingId/status") {
        authorize()
        setBody(request)
    }.bodyOrThrow()

    suspend fun uploadListingImages(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        images: List<ListingImageUpload>,
    ): List<ListingImageResponseDto> = httpClient.post("/api/listings/$listingId/images") {
        authorize()
        contentType(ContentType.MultiPart.FormData)
        setBody(
            MultiPartFormDataContent(
                formData {
                    images.forEach { image ->
                        append(
                            key = "files",
                            value = image.bytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"files\"; filename=\"${image.fileName.safeMultipartFileName()}\"",
                                )
                                append(HttpHeaders.ContentType, image.mimeType)
                            },
                        )
                    }
                },
            ),
        )
    }.bodyOrThrow()

    suspend fun deleteListingImage(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        imageId: String,
    ) {
        httpClient.delete("/api/listings/$listingId/images/$imageId") {
            authorize()
        }.bodyOrThrow<Unit>()
    }

    suspend fun updateListingImagesOrder(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        request: UpdateListingImagesOrderRequestDto,
    ) {
        httpClient.put("/api/listings/$listingId/images/order") {
            authorize()
            setBody(request)
        }.bodyOrThrow<Unit>()
    }
}

private fun String.safeMultipartFileName(): String {
    val sanitized = map { character ->
        when {
            character == '"' || character == '\\' || character == '/' -> '_'
            character.code < 0x20 || character.code == 0x7F -> '_'
            else -> character
        }
    }.joinToString("").trim()

    return sanitized.ifEmpty { "image" }
}
