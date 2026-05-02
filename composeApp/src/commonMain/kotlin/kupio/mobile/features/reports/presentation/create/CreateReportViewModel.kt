package kupio.mobile.features.reports.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kupio.mobile.core.analytics.AnalyticsService
import kupio.mobile.core.analytics.NoOpAnalyticsService
import kupio.mobile.features.reports.domain.repository.ReportsRepository

class CreateReportViewModel(
    private val listingId: String,
    private val listingTitle: String,
    private val listingImageUrl: String,
    private val listingPriceFormatted: String,
    private val reportsRepository: ReportsRepository,
    private val analytics: AnalyticsService = NoOpAnalyticsService(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        CreateReportState(
            listingTitle = listingTitle,
            listingImageUrl = listingImageUrl,
            listingPriceFormatted = listingPriceFormatted,
        )
    )
    val state: StateFlow<CreateReportState> = _state.asStateFlow()

    private val effectChannel = Channel<CreateReportEffect>(Channel.BUFFERED)
    val effects: Flow<CreateReportEffect> = effectChannel.receiveAsFlow()

    init {
        loadReasons()
    }

    fun onIntent(intent: CreateReportIntent) {
        when (intent) {
            is CreateReportIntent.SelectReason -> _state.update {
                val newId = if (it.selectedReasonId == intent.id) null else intent.id
                it.copy(selectedReasonId = newId, submitError = null)
            }
            is CreateReportIntent.AdditionalInfoChanged -> _state.update {
                it.copy(additionalInfo = intent.text, submitError = null)
            }
            CreateReportIntent.Submit -> submit()
            CreateReportIntent.Back -> viewModelScope.launch {
                effectChannel.send(CreateReportEffect.NavigateBack)
            }
            CreateReportIntent.RetryReasons -> loadReasons()
        }
    }

    private fun loadReasons() {
        _state.update { it.copy(isLoadingReasons = true, reasonsError = null) }
        viewModelScope.launch {
            runCatching {
                reportsRepository.getReportReasons()
            }.onSuccess { reasons ->
                _state.update { it.copy(reasons = reasons, isLoadingReasons = false) }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(isLoadingReasons = false, reasonsError = throwable.message)
                }
                analytics.recordException(throwable, mapOf("screen" to "create_report", "action" to "load_reasons"))
            }
        }
    }

    private fun submit() {
        val reasonId = _state.value.selectedReasonId ?: return
        if (_state.value.isSubmitting) return

        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            runCatching {
                reportsRepository.createListingReport(
                    listingId = listingId,
                    reasonId = reasonId,
                    additionalInfo = _state.value.additionalInfo.trim().takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                analytics.logEvent("report_listing", mapOf("item_id" to listingId, "reason_id" to reasonId.toString()))
                _state.update { it.copy(isSubmitting = false) }
                effectChannel.send(CreateReportEffect.NavigateBack)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(isSubmitting = false, submitError = throwable.message)
                }
                analytics.recordException(throwable, mapOf("screen" to "create_report", "item_id" to listingId))
            }
        }
    }
}

