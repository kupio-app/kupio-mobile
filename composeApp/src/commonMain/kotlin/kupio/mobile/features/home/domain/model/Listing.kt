package kupio.mobile.features.home.domain.model

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

enum class Currency(val symbol: String) {
    USD("$"),
    EUR("€"),
    CZK("Kč"),
    UAH("₴"),
}

fun Listing.formatPrice(): String = "$price ${currency.symbol}"
