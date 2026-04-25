package kupio.mobile.features.listings.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed

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
    val status: ListingStatusDto = ListingStatusDto.ACTIVE,
    @SerialName("user_id") val userId: String,
    val category: CategorySlimDto,
    val images: List<ListingImageResponseDto> = emptyList(),
    @SerialName("created_at") val createdAt: String,
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

fun ListingResponseDto.toDomain(): Listing = Listing(
    id = id,
    title = title,
    description = description,
    price = price,
    currency = currency.toDomain(),
    primaryImageUrl = images.minByOrNull { it.sortOrder }?.url,
    createdAt = createdAt,
    categoryId = category.id,
    categoryName = category.name,
)

fun ListListingsResponseDto.toDomain(): ListingFeed = ListingFeed(
    listings = listings.map { it.toDomain() },
    nextCursor = nextCursor,
)
