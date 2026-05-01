package kupio.mobile.features.reports.presentation.moderator

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.reports.domain.model.ReportListItem
import kupio.mobile.features.reports.domain.model.ReportsDashboardStats

data class ModeratorReportsDashboardState(
    val filter: ReportsDashboardFilter = ReportsDashboardFilter.IN_QUEUE,
    val reports: List<ReportListItem> = emptyList(),
    val stats: ReportsDashboardStats? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = false,
) : UiState

enum class ReportsDashboardFilter { IN_QUEUE, UNSEEN, NO_ACTION }

fun ReportsDashboardFilter.toApiParams(): Pair<String?, String?> = when (this) {
    ReportsDashboardFilter.IN_QUEUE -> Pair("pending", null)
    ReportsDashboardFilter.UNSEEN -> Pair("pending", "unseen")
    ReportsDashboardFilter.NO_ACTION -> Pair("pending", "seen")
}

sealed interface ModeratorReportsDashboardIntent : UiAction {
    data class FilterSelected(val filter: ReportsDashboardFilter) : ModeratorReportsDashboardIntent
    data object BackClicked : ModeratorReportsDashboardIntent
    data object Refresh : ModeratorReportsDashboardIntent
    data object LoadMore : ModeratorReportsDashboardIntent
    data object RetryLoad : ModeratorReportsDashboardIntent
    data class ReportClicked(val id: Int) : ModeratorReportsDashboardIntent
}

sealed interface ModeratorReportsDashboardEffect : UiEffect {
    data object NavigateBack : ModeratorReportsDashboardEffect
    data class NavigateToReportDetail(val reportId: Int) : ModeratorReportsDashboardEffect
}

