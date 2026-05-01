package kupio.mobile.features.reports.domain.model

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
)

data class ListReportsResult(
    val stats: ReportsDashboardStats,
    val reports: List<ReportListItem>,
    val nextCursor: String?,
)

