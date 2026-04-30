package kupio.mobile.features.listings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingStatus

@Serializable
data class ListListingsResponseDto(
    val listings: List<ListingResponseDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class ListingResponseDto(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val currency: CurrencyDto,
    val status: ListingStatusDto,
    @SerialName("user_id") val userId: String,
    val category: CategorySlimDto,
    val images: List<ListingImageResponseDto> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("seen_count") val seenCount: Int = 0,
    val phone: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    @SerialName("is_calls_disabled") val isCallsDisabled: Boolean = false,
    @SerialName("custom_filters") val customFilters: JsonObject? = null,
)

@Serializable
data class ListingImageResponseDto(
    val id: String,
    val url: String,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
data class CategorySlimDto(
    val id: Int,
    val name: String,
    val depth: Int,
    val icon: String? = null,
)

@Serializable
data class ListingRequestDto(
    val title: String,
    val description: String,
    val price: Int,
    @SerialName("is_free") val isFree: Boolean = false,
    @SerialName("is_tradable") val isTradable: Boolean = false,
    val currency: CurrencyDto,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("custom_filters") val customFilters: JsonObject? = null,
)

@Serializable
data class ListingStatusUpdateRequestDto(
    val status: ListingStatusDto,
)

@Serializable
enum class CurrencyDto {
    @SerialName("usd") USD,
    @SerialName("eur") EUR,
    @SerialName("czk") CZK,
    @SerialName("uah") UAH,
}

@Serializable
enum class ListingStatusDto {
    @SerialName("draft") DRAFT,
    @SerialName("planned") PLANNED,
    @SerialName("active") ACTIVE,
    @SerialName("inactive") INACTIVE,
    @SerialName("sold") SOLD,
}

fun CurrencyDto.toDomain(): Currency = when (this) {
    CurrencyDto.USD -> Currency.USD
    CurrencyDto.EUR -> Currency.EUR
    CurrencyDto.CZK -> Currency.CZK
    CurrencyDto.UAH -> Currency.UAH
}

fun Currency.toDto(): CurrencyDto = when (this) {
    Currency.USD -> CurrencyDto.USD
    Currency.EUR -> CurrencyDto.EUR
    Currency.CZK -> CurrencyDto.CZK
    Currency.UAH -> CurrencyDto.UAH
}

fun ListingStatus.toDto(): ListingStatusDto = when (this) {
    ListingStatus.DRAFT -> ListingStatusDto.DRAFT
    ListingStatus.PLANNED -> ListingStatusDto.PLANNED
    ListingStatus.ACTIVE -> ListingStatusDto.ACTIVE
    ListingStatus.INACTIVE -> ListingStatusDto.INACTIVE
    ListingStatus.SOLD -> ListingStatusDto.SOLD
}

fun ListingStatusDto.toDomain(): ListingStatus = when (this) {
    ListingStatusDto.DRAFT -> ListingStatus.DRAFT
    ListingStatusDto.PLANNED -> ListingStatus.PLANNED
    ListingStatusDto.ACTIVE -> ListingStatus.ACTIVE
    ListingStatusDto.INACTIVE -> ListingStatus.INACTIVE
    ListingStatusDto.SOLD -> ListingStatus.SOLD
}

fun ListingResponseDto.toDomain(): Listing = Listing(
    id = id,
    title = title,
    description = description,
    price = price,
    currency = currency.toDomain(),
    status = status.toDomain(),
    primaryImageUrl = images.minByOrNull { it.sortOrder }?.url,
    imageUrls = images.sortedBy { it.sortOrder }.map { it.url },
    createdAt = createdAt,
    userId = userId,
    categoryId = category.id,
    categoryName = category.name,
    seenCount = seenCount,
    phone = phone,
    contactName = contactName,
    isCallsDisabled = isCallsDisabled,
    customFilters = customFilters.toDisplayMap(),
)

fun ListListingsResponseDto.toDomain(): ListingFeed = ListingFeed(
    listings = listings.map { it.toDomain() },
    nextCursor = nextCursor,
)

fun CreateListing.toDto(): ListingRequestDto = ListingRequestDto(
    title = title,
    description = description,
    price = price,
    isFree = isFree,
    isTradable = isTradable,
    currency = currency.toDto(),
    categoryId = categoryId,
    customFilters = customFilters.toJsonObject().takeUnless { it.isEmpty() },
)

fun Map<String, CustomFilterPayloadValue>.toJsonObject(): JsonObject = buildJsonObject {
    this@toJsonObject.forEach { (key, value) ->
        put(key, value.toJsonElement())
    }
}

private fun CustomFilterPayloadValue.toJsonElement(): JsonElement = when (this) {
    is CustomFilterPayloadValue.Text -> JsonPrimitive(value)
    is CustomFilterPayloadValue.Number -> JsonPrimitive(value)
    is CustomFilterPayloadValue.BooleanValue -> JsonPrimitive(value)
}

private fun JsonObject?.toDisplayMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return entries.mapNotNull { (key, value) ->
        val text = when (value) {
            is JsonPrimitive -> value.contentOrNull
            else -> value.toString()
        }?.takeIf { it.isNotBlank() }
        text?.let { key to it }
    }.toMap()
}
