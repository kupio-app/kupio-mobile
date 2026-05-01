package kupio.mobile.features.reports.domain.repository

import kupio.mobile.features.reports.domain.model.ListReportsResult
import kupio.mobile.features.reports.domain.model.ReportReason

interface ReportsRepository {
    suspend fun getReportReasons(): List<ReportReason>
    suspend fun createListingReport(
        listingId: String,
        reasonId: Int,
        additionalInfo: String?,
    )
    suspend fun getReports(
        status: String? = null,
        seen: String? = null,
        cursor: String? = null,
    ): ListReportsResult
}

