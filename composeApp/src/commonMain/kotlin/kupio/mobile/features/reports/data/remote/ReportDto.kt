package kupio.mobile.features.reports.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kupio.mobile.features.reports.domain.model.ReportReason

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

