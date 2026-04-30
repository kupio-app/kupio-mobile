package kupio.mobile.features.me.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.listings.data.remote.CurrencyDto
import kupio.mobile.features.listings.data.remote.ListingStatusDto
import kupio.mobile.features.listings.data.remote.toDomain
import kupio.mobile.features.me.domain.model.OwnedListing
import kupio.mobile.features.me.domain.model.OwnedListingsPage
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.domain.model.UserListingStats

@Serializable
data class UserListingStatsDto(
    @SerialName("active_count") val activeCount: Int,
    @SerialName("inactive_count") val inactiveCount: Int,
    @SerialName("promoted_count") val promotedCount: Int,
    @SerialName("chats_count") val chatsCount: Int,
    @SerialName("favourites_count") val favouritesCount: Int,
)

@Serializable
data class OwnerListingImageDto(
    val id: String,
    val url: String,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
data class OwnerListingResponseDto(
    val id: String,
    val title: String,
    val price: Int,
    val currency: CurrencyDto,
    val status: ListingStatusDto,
    val images: List<OwnerListingImageDto> = emptyList(),
    @SerialName("seen_count") val seenCount: Int,
    @SerialName("favourites_count") val favouritesCount: Int,
    @SerialName("chats_count") val chatsCount: Int,
    @SerialName("is_promoted") val isPromoted: Boolean,
    @SerialName("promotion_expires_at") val promotionExpiresAt: String?,
)

@Serializable
data class ListOwnerListingsResponseDto(
    val listings: List<OwnerListingResponseDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class UpdateListingStatusRequestDto(
    val status: ListingStatusDto,
)

fun UserListingStatsDto.toDomain() = UserListingStats(
    activeCount = activeCount,
    inactiveCount = inactiveCount,
    promotedCount = promotedCount,
    chatsCount = chatsCount,
    favouritesCount = favouritesCount,
)

fun OwnerListingResponseDto.toDomain() = OwnedListing(
    id = id,
    title = title,
    price = price,
    currency = currency.toDomain(),
    primaryImageUrl = images.minByOrNull { it.sortOrder }?.url,
    status = status.toOwnedDomain(),
    seenCount = seenCount,
    favouritesCount = favouritesCount,
    chatsCount = chatsCount,
    isPromoted = isPromoted,
    promotionExpiresAt = promotionExpiresAt,
)

fun ListOwnerListingsResponseDto.toDomain() = OwnedListingsPage(
    listings = listings.map { it.toDomain() },
    nextCursor = nextCursor,
)

fun ListingStatusDto.toOwnedDomain(): OwnedListingStatus = when (this) {
    ListingStatusDto.ACTIVE -> OwnedListingStatus.ACTIVE
    ListingStatusDto.INACTIVE -> OwnedListingStatus.INACTIVE
    ListingStatusDto.DRAFT -> OwnedListingStatus.DRAFT
    ListingStatusDto.PLANNED -> OwnedListingStatus.PLANNED
    ListingStatusDto.SOLD -> OwnedListingStatus.SOLD
}

fun OwnedListingStatus.toDto(): ListingStatusDto = when (this) {
    OwnedListingStatus.ACTIVE -> ListingStatusDto.ACTIVE
    OwnedListingStatus.INACTIVE -> ListingStatusDto.INACTIVE
    OwnedListingStatus.DRAFT -> ListingStatusDto.DRAFT
    OwnedListingStatus.PLANNED -> ListingStatusDto.PLANNED
    OwnedListingStatus.SOLD -> ListingStatusDto.SOLD
}
