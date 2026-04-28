package kupio.mobile.features.me.domain.model

import kupio.mobile.features.listings.domain.model.Currency

data class OwnedListing(
    val id: String,
    val title: String,
    val price: Int,
    val currency: Currency,
    val primaryImageUrl: String?,
    val status: OwnedListingStatus,
    val seenCount: Int,
    val favouritesCount: Int,
    val chatsCount: Int,
    val isPromoted: Boolean,
    val promotionExpiresAt: String?,
)

enum class OwnedListingStatus { ACTIVE, INACTIVE, DRAFT, PLANNED, SOLD }

fun OwnedListing.formatPrice(): String =
    if (currency.symbolFirst) "${currency.symbol}$price" else "$price ${currency.symbol}"
