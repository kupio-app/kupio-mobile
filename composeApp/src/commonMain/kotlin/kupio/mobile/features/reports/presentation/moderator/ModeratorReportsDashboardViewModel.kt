package kupio.mobile.features.reports.presentation.moderator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService
import kupio.mobile.features.reports.domain.model.ReportListItem
import kupio.mobile.features.reports.domain.model.ReportsDashboardStats
import kupio.mobile.features.reports.domain.repository.ReportsRepository

class ModeratorReportsDashboardViewModel(
    private val reportsRepository: ReportsRepository,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {

    private val _state = MutableStateFlow(ModeratorReportsDashboardState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<ModeratorReportsDashboardEffect>(Channel.BUFFERED)
    val effects: Flow<ModeratorReportsDashboardEffect> = effectChannel.receiveAsFlow()

    private var nextCursor: String? = null
    private var loadJob: Job? = null

    init {
        loadReports(reset = true)
    }

    fun onIntent(intent: ModeratorReportsDashboardIntent) {
        when (intent) {
            is ModeratorReportsDashboardIntent.FilterSelected -> {
                if (intent.filter != _state.value.filter) {
                    _state.update { it.copy(filter = intent.filter) }
                    analytics.logEvent("filter_reports", mapOf("filter" to intent.filter.name))
                    loadReports(reset = true)
                }
            }
            ModeratorReportsDashboardIntent.BackClicked ->
                emitEffect(ModeratorReportsDashboardEffect.NavigateBack)
            ModeratorReportsDashboardIntent.Refresh -> refreshReports()
            ModeratorReportsDashboardIntent.LoadMore -> loadMore()
            ModeratorReportsDashboardIntent.RetryLoad -> loadReports(reset = true)
            is ModeratorReportsDashboardIntent.ReportClicked ->
                emitEffect(ModeratorReportsDashboardEffect.NavigateToReportDetail(intent.id))
        }
    }

    private fun refreshReports() {
        _state.update { it.copy(isRefreshing = true) }
        loadReports(reset = true, isRefresh = true)
    }

    private fun loadMore() {
        val currentState = _state.value
        if (!currentState.hasMore || currentState.isLoadingMore || currentState.isLoading) return
        _state.update { it.copy(isLoadingMore = true) }
        loadReports(reset = false)
    }

    private fun loadReports(reset: Boolean, isRefresh: Boolean = false) {
        if (reset) {
            loadJob?.cancel()
            nextCursor = null
            if (!isRefresh) {
                _state.update {
                    it.copy(
                        reports = emptyList(),
                        isLoading = true,
                        errorMessage = null,
                        hasMore = false,
                    )
                }
            }
        }

        val filter = _state.value.filter
        val (status, seen) = filter.toApiParams()
        val cursor = if (reset) null else nextCursor

        loadJob = viewModelScope.launch {
            runCatching {
                reportsRepository.getReports(
                    status = status,
                    seen = seen,
                    cursor = cursor,
                )
            }.onSuccess { result ->
                nextCursor = result.nextCursor
                val newReports = if (reset) {
                    result.reports
                } else {
                    _state.value.reports + result.reports
                }
                val hasMore = result.nextCursor != null
                val statsToApply = when (filter) {
                    ReportsDashboardFilter.IN_QUEUE -> result.stats.withQueueCountsFrom(
                        reports = newReports,
                        hasMore = hasMore,
                    )
                    ReportsDashboardFilter.UNSEEN,
                    ReportsDashboardFilter.NO_ACTION -> _state.value.stats ?: result.stats
                }
                _state.update {
                    it.copy(
                        reports = newReports,
                        stats = statsToApply,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = hasMore,
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = t.message ?: "Unknown error",
                    )
                }
                analytics.recordException(t, mapOf("screen" to "reports_dashboard"))
            }
        }
    }

    private fun emitEffect(effect: ModeratorReportsDashboardEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}

private fun ReportsDashboardStats.withQueueCountsFrom(
    reports: List<ReportListItem>,
    hasMore: Boolean,
): ReportsDashboardStats {
    if (hasMore) return this
    return copy(
        noAction = reports.count { it.seen },
        unseen = reports.count { !it.seen },
    )
}
