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
import kupio.mobile.features.reports.domain.repository.ReportsRepository

class ModeratorReportDetailViewModel(
    private val reportId: Int,
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ModeratorReportDetailState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<ModeratorReportDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ModeratorReportDetailEffect> = effectChannel.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onIntent(intent: ModeratorReportDetailIntent) {
        when (intent) {
            ModeratorReportDetailIntent.BackClicked ->
                emitEffect(ModeratorReportDetailEffect.NavigateBack)
            ModeratorReportDetailIntent.Retry -> loadDetail()
            is ModeratorReportDetailIntent.DecisionSelected ->
                _state.update { it.copy(selectedDecision = intent.decision, submitError = null) }
            is ModeratorReportDetailIntent.CommentChanged ->
                _state.update { it.copy(moderatorComment = intent.text) }
            ModeratorReportDetailIntent.SubmitDecision -> submitDecision()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                reportsRepository.getReportDetail(reportId)
            }.onSuccess { report ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        errorMessage = null,
                        // pre-fill moderator comment if already moderated
                        moderatorComment = report.moderatorComment.orEmpty(),
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    private fun submitDecision() {
        val decision = _state.value.selectedDecision
        if (decision == null) {
            _state.update { it.copy(submitError = "Please select a decision") }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        val comment = _state.value.moderatorComment
        viewModelScope.launch {
            runCatching {
                reportsRepository.submitDecision(
                    reportId = reportId,
                    action = decision.toApiValue(),
                    comment = comment,
                )
            }.onSuccess { updatedReport ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        report = updatedReport,
                        submitError = null,
                    )
                }
                emitEffect(ModeratorReportDetailEffect.ShowSuccess("Decision submitted"))
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = t.message ?: "Failed to submit decision",
                    )
                }
            }
        }
    }

    private fun emitEffect(effect: ModeratorReportDetailEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
