package kupio.mobile.features.reports.presentation.moderator

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.reports.domain.model.ListReportsResult
import kupio.mobile.features.reports.domain.model.ReportDetail
import kupio.mobile.features.reports.domain.model.ReportListItem
import kupio.mobile.features.reports.domain.model.ReportReason
import kupio.mobile.features.reports.domain.model.ReportStatus
import kupio.mobile.features.reports.domain.model.ReportsDashboardStats
import kupio.mobile.features.reports.domain.repository.ReportsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ModeratorReportsDashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `complete queue load derives tab counts from loaded reports and keeps them across filters`() = runTest(dispatcher) {
        val repository = FakeReportsRepository(
            inQueueResult = reportsResult(
                stats = ReportsDashboardStats(newToday = 4, noAction = 99, unseen = 88),
                reports = listOf(
                    report(id = 1, seen = false),
                    report(id = 2, seen = false),
                    report(id = 3, seen = true),
                ),
            ),
            unseenResult = reportsResult(
                stats = ReportsDashboardStats(newToday = 4, noAction = 0, unseen = 123),
                reports = listOf(
                    report(id = 1, seen = false),
                    report(id = 2, seen = false),
                ),
            ),
        )

        val viewModel = ModeratorReportsDashboardViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.stats?.unseen)
        assertEquals(1, viewModel.state.value.stats?.noAction)

        viewModel.onIntent(ModeratorReportsDashboardIntent.FilterSelected(ReportsDashboardFilter.UNSEEN))
        advanceUntilIdle()

        assertEquals(listOf(false, false), viewModel.state.value.reports.map { it.seen })
        assertEquals(2, viewModel.state.value.stats?.unseen)
        assertEquals(1, viewModel.state.value.stats?.noAction)
    }

    @Test
    fun `partial queue load keeps api stats until all pages are loaded`() = runTest(dispatcher) {
        val repository = FakeReportsRepository(
            inQueueResult = reportsResult(
                stats = ReportsDashboardStats(newToday = 4, noAction = 12, unseen = 8),
                reports = listOf(report(id = 1, seen = false)),
                nextCursor = "next-page",
            ),
        )

        val viewModel = ModeratorReportsDashboardViewModel(repository)
        advanceUntilIdle()

        assertEquals(8, viewModel.state.value.stats?.unseen)
        assertEquals(12, viewModel.state.value.stats?.noAction)
    }

    @Test
    fun `not found filtered response is treated as error`() = runTest(dispatcher) {
        val repository = FakeReportsRepository(
            inQueueResult = reportsResult(
                reports = listOf(
                    report(id = 1, seen = false),
                    report(id = 2, seen = true),
                ),
            ),
            unseenError = ApiException(statusCode = 404, message = "No reports"),
        )

        val viewModel = ModeratorReportsDashboardViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(ModeratorReportsDashboardIntent.FilterSelected(ReportsDashboardFilter.UNSEEN))
        advanceUntilIdle()

        assertEquals("No reports", viewModel.state.value.errorMessage)
        assertEquals(1, viewModel.state.value.stats?.unseen)
        assertEquals(1, viewModel.state.value.stats?.noAction)
    }

    private class FakeReportsRepository(
        private val inQueueResult: ListReportsResult = reportsResult(),
        private val unseenResult: ListReportsResult = reportsResult(),
        private val noActionResult: ListReportsResult = reportsResult(),
        private val unseenError: Throwable? = null,
        private val noActionError: Throwable? = null,
    ) : ReportsRepository {
        override suspend fun getReportReasons(): List<ReportReason> = error("unused")

        override suspend fun createListingReport(
            listingId: String,
            reasonId: Int,
            additionalInfo: String?,
        ) = error("unused")

        override suspend fun getReports(
            status: String?,
            seen: String?,
            cursor: String?,
        ): ListReportsResult = when (seen) {
            "unseen" -> unseenError?.let { throw it } ?: unseenResult
            "seen" -> noActionError?.let { throw it } ?: noActionResult
            else -> inQueueResult
        }

        override suspend fun getReportDetail(reportId: Int): ReportDetail = error("unused")

        override suspend fun submitDecision(
            reportId: Int,
            action: String,
            comment: String?,
        ): ReportDetail = error("unused")
    }
}

private fun reportsResult(
    stats: ReportsDashboardStats = ReportsDashboardStats(newToday = 0, noAction = 0, unseen = 0),
    reports: List<ReportListItem> = emptyList(),
    nextCursor: String? = null,
) = ListReportsResult(
    stats = stats,
    reports = reports,
    nextCursor = nextCursor,
)

private fun report(
    id: Int,
    seen: Boolean,
) = ReportListItem(
    id = id,
    status = ReportStatus.PENDING,
    createdAt = "2026-05-01T00:00:00Z",
    seen = seen,
    reasonTitle = "Spam",
    reasonDescription = null,
    additionalInfoPreview = null,
    listingId = "listing-$id",
    listingTitle = "Listing $id",
    listingImageUrl = null,
    sellerUsername = "seller$id",
    sellerDisplayName = "Seller $id",
)
