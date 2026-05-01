package kupio.mobile.features.reports.presentation.moderator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.reports.domain.repository.ReportsRepository

class ModeratorReportsDashboardViewModel(
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ModeratorReportsDashboardState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<ModeratorReportsDashboardEffect>(Channel.BUFFERED)
    val effects: Flow<ModeratorReportsDashboardEffect> = effectChannel.receiveAsFlow()

    private var nextCursor: String? = null

    init {
        loadReports(reset = true)
    }

    fun onIntent(intent: ModeratorReportsDashboardIntent) {
        when (intent) {
            is ModeratorReportsDashboardIntent.FilterSelected -> {
                if (intent.filter != _state.value.filter) {
                    _state.update { it.copy(filter = intent.filter) }
                    loadReports(reset = true)
                }
            }
            ModeratorReportsDashboardIntent.BackClicked ->
                emitEffect(ModeratorReportsDashboardEffect.NavigateBack)
            ModeratorReportsDashboardIntent.Refresh -> refreshReports()
            ModeratorReportsDashboardIntent.LoadMore -> loadMore()
            ModeratorReportsDashboardIntent.RetryLoad -> loadReports(reset = true)
            is ModeratorReportsDashboardIntent.ReportClicked -> Unit // no-op for now
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
            nextCursor = null
            if (!isRefresh) {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            }
        }

        val filter = _state.value.filter
        val (status, seen) = filter.toApiParams()
        val cursor = if (reset) null else nextCursor

        viewModelScope.launch {
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
                _state.update {
                    it.copy(
                        reports = newReports,
                        stats = result.stats,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        hasMore = result.nextCursor != null,
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                // Treat 404 as empty result (no reports for this filter)
                if (t is ApiException && t.statusCode == 404) {
                    _state.update {
                        it.copy(
                            reports = if (reset) emptyList() else it.reports,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            errorMessage = null,
                            hasMore = false,
                        )
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = t.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    private fun emitEffect(effect: ModeratorReportsDashboardEffect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}

