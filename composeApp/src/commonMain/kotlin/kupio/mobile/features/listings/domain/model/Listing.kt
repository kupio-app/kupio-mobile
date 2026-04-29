package kupio.mobile.features.listings.domain.model

data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val currency: Currency,
    val primaryImageUrl: String?,
    val imageUrls: List<String>,
    val createdAt: String,
    val userId: String,
    val categoryId: Int,
    val categoryName: String,
    val seenCount: Int,
    val phone: String?,
    val contactName: String?,
    val isCallsDisabled: Boolean,
    val customFilters: Map<String, String>,
)

data class ListingFeed(
    val listings: List<Listing>,
    val nextCursor: String?,
)

enum class ListingStatus {
    DRAFT,
    PLANNED,
    ACTIVE,
    INACTIVE,
    SOLD,
}

data class CreateListing(
    val title: String,
    val description: String,
    val price: Int,
    val currency: Currency,
    val categoryId: Int,
    val isFree: Boolean,
    val isTradable: Boolean,
    val customFilters: Map<String, CustomFilterPayloadValue>,
)

data class ListingImageUpload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListingImageUpload) return false

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

sealed interface CustomFilterPayloadValue {
    data class Text(val value: String) : CustomFilterPayloadValue
    data class Number(val value: Double) : CustomFilterPayloadValue
    data class BooleanValue(val value: Boolean) : CustomFilterPayloadValue
}

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
