package kupio.mobile.features.reports.domain.model

import kupio.mobile.features.listings.domain.model.Currency

data class ReportListingDetail(
    val id: String,
    val title: String,
    val description: String,
    val price: Int,
    val currency: Currency,
    val status: String,
    val primaryImageUrl: String?,
)

fun ReportListingDetail.formatPrice(): String =
    if (currency.symbolFirst) "${currency.symbol}$price" else "$price ${currency.symbol}"

data class ReportDetail(
    val id: Int,
    val status: ReportStatus,
    val createdAt: String,
    val additionalInfo: String?,
    val reasonTitle: String,
    val reasonDescription: String?,
    val listing: ReportListingDetail,
    val sellerUsername: String,
    val sellerDisplayName: String,
    val sellerId: String,
    val sellerCreatedAt: String?,
    val moderatedAt: String?,
    val moderatorComment: String?,
)

data class ReportListItem(
    val id: Int,
    val status: ReportStatus,
    val createdAt: String,
    val seen: Boolean,
    val reasonTitle: String,
    val reasonDescription: String?,
    val additionalInfoPreview: String?,
    val listingId: String,
    val listingTitle: String,
    val listingImageUrl: String?,
    val sellerUsername: String,
    val sellerDisplayName: String,
)

enum class ReportStatus {
    PENDING, DECLINED, LISTING_REMOVED, USER_BANNED, UNKNOWN;

    companion object {
        fun fromString(value: String): ReportStatus = when (value) {
            "pending" -> PENDING
            "declined" -> DECLINED
            "listing_removed" -> LISTING_REMOVED
            "user_banned" -> USER_BANNED
            else -> UNKNOWN
        }
    }
}

data class ReportsDashboardStats(
    val newToday: Int,
    val noAction: Int,
    val unseen: Int,
) {
    val inQueue: Int
        get() = unseen + noAction
}

data class ListReportsResult(
    val stats: ReportsDashboardStats,
    val reports: List<ReportListItem>,
    val nextCursor: String?,
)
