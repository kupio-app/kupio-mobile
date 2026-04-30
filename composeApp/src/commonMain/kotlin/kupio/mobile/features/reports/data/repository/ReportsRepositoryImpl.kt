package kupio.mobile.features.reports.data.repository

import kupio.mobile.core.network.AuthenticatedApiClient
import kupio.mobile.features.reports.data.remote.CreateListingReportRequestDto
import kupio.mobile.features.reports.data.remote.ReportsApi
import kupio.mobile.features.reports.data.remote.toDomain
import kupio.mobile.features.reports.domain.model.ReportReason
import kupio.mobile.features.reports.domain.repository.ReportsRepository

class ReportsRepositoryImpl(
    private val reportsApi: ReportsApi,
    private val authenticatedApiClient: AuthenticatedApiClient,
) : ReportsRepository {

    override suspend fun getReportReasons(): List<ReportReason> =
        reportsApi.getReportReasons().map { it.toDomain() }

    override suspend fun createListingReport(
        listingId: String,
        reasonId: Int,
        additionalInfo: String?,
    ) {
        authenticatedApiClient.request { authorize ->
            reportsApi.createListingReport(
                authorize = authorize,
                listingId = listingId,
                request = CreateListingReportRequestDto(
                    reasonId = reasonId,
                    additionalInfo = additionalInfo?.takeIf { it.isNotBlank() },
                ),
            )
        }
    }
}

