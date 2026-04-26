package kupio.mobile.features.listings.domain.model

data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val currency: Currency,
    val primaryImageUrl: String?,
    val createdAt: String,
    val categoryId: Int,
    val categoryName: String,
)

data class ListingFeed(
    val listings: List<Listing>,
    val nextCursor: String?,
)

enum class Currency(
    val symbol: String,
    val symbolFirst: Boolean,
) {
    USD("$", true),
    EUR("€", true),
    CZK("Kč", false),
    UAH("₴", true),
}

fun Listing.formatPrice(): String =
    if (currency.symbolFirst) "${currency.symbol}$price" else "$price ${currency.symbol}"
