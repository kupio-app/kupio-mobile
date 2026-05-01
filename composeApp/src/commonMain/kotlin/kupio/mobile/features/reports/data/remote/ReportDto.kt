package kupio.mobile.features.reports.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.reports.domain.model.ListReportsResult
import kupio.mobile.features.reports.domain.model.ReportListItem
import kupio.mobile.features.reports.domain.model.ReportReason
import kupio.mobile.features.reports.domain.model.ReportsDashboardStats
import kupio.mobile.features.reports.domain.model.ReportStatus

@Serializable
data class ReportReasonResponseDto(
    val id: Int,
    val slug: String,
    val title: String,
    val description: String? = null,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class CreateListingReportRequestDto(
    @SerialName("reason_id") val reasonId: Int,
    @SerialName("additional_info") val additionalInfo: String? = null,
)

@Serializable
data class CreatedListingReportResponseDto(
    val id: Int,
    @SerialName("listing_id") val listingId: String,
)

fun ReportReasonResponseDto.toDomain(): ReportReason = ReportReason(
    id = id,
    slug = slug,
    title = title,
    description = description,
)

@Serializable
data class ReportsDashboardStatsDto(
    @SerialName("new_today") val newToday: Int,
    @SerialName("no_action") val noAction: Int,
    val unseen: Int,
)

@Serializable
data class ReportListingSummaryDto(
    val id: String,
    val title: String,
    val price: Int,
    val currency: String,
    val status: String,
    @SerialName("primary_image_url") val primaryImageUrl: String? = null,
)

@Serializable
data class ReportSellerSummaryDto(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class ReportReasonSummaryDto(
    val id: Int,
    val slug: String,
    val title: String,
    val description: String? = null,
)

@Serializable
data class ReportListItemDto(
    val id: Int,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val seen: Boolean,
    val reason: ReportReasonSummaryDto,
    @SerialName("additional_info_preview") val additionalInfoPreview: String? = null,
    val listing: ReportListingSummaryDto,
    val seller: ReportSellerSummaryDto,
)

@Serializable
data class ListReportsResponseDto(
    val stats: ReportsDashboardStatsDto,
    val reports: List<ReportListItemDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

fun ReportListItemDto.toDomain(): ReportListItem = ReportListItem(
    id = id,
    status = ReportStatus.fromString(status),
    createdAt = createdAt,
    seen = seen,
    reasonTitle = reason.title,
    reasonDescription = reason.description,
    additionalInfoPreview = additionalInfoPreview,
    listingId = listing.id,
    listingTitle = listing.title,
    listingImageUrl = listing.primaryImageUrl,
    sellerUsername = seller.username.orEmpty(),
    sellerDisplayName = seller.displayName.orEmpty(),
)

fun ListReportsResponseDto.toDomain(): ListReportsResult = ListReportsResult(
    stats = ReportsDashboardStats(
        newToday = stats.newToday,
        noAction = stats.noAction,
        unseen = stats.unseen,
    ),
    reports = reports.map { it.toDomain() },
    nextCursor = nextCursor,
)
