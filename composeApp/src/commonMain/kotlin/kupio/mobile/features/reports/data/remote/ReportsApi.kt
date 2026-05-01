package kupio.mobile.features.reports.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kupio.mobile.core.network.bodyOrThrow

class ReportsApi(private val httpClient: HttpClient) {

    suspend fun getReportReasons(): List<ReportReasonResponseDto> =
        httpClient.get("/api/reports/reasons").bodyOrThrow()

    suspend fun createListingReport(
        authorize: HttpRequestBuilder.() -> Unit,
        listingId: String,
        request: CreateListingReportRequestDto,
    ): CreatedListingReportResponseDto = httpClient.post("/api/listings/$listingId/reports") {
        authorize()
        setBody(request)
    }.bodyOrThrow()

    suspend fun getReports(
        authorize: HttpRequestBuilder.() -> Unit,
        status: String? = null,
        seen: String? = null,
        cursor: String? = null,
        limit: Int = 20,
    ): ListReportsResponseDto = httpClient.get("/api/reports") {
        authorize()
        if (status != null) parameter("status", status)
        if (seen != null) parameter("seen", seen)
        if (cursor != null) parameter("cursor", cursor)
        parameter("limit", limit)
    }.bodyOrThrow()

    suspend fun getReportDetail(
        authorize: HttpRequestBuilder.() -> Unit,
        reportId: Int,
    ): ReportDetailResponseDto = httpClient.get("/api/reports/$reportId") {
        authorize()
    }.bodyOrThrow()

    suspend fun submitDecision(
        authorize: HttpRequestBuilder.() -> Unit,
        reportId: Int,
        request: ModerateReportRequestDto,
    ): ReportDetailResponseDto = httpClient.post("/api/reports/$reportId/decision") {
        authorize()
        setBody(request)
    }.bodyOrThrow()
}

