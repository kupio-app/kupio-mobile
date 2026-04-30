package kupio.mobile.features.reports.presentation.create

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.reports.domain.model.ReportReason

data class CreateReportState(
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val listingPriceFormatted: String = "",
    val reasons: List<ReportReason> = emptyList(),
    val isLoadingReasons: Boolean = true,
    val reasonsError: String? = null,
    val selectedReasonId: Int? = null,
    val additionalInfo: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
) : UiState {
    val canSubmit: Boolean
        get() = selectedReasonId != null && !isSubmitting && !isLoadingReasons
}

sealed interface CreateReportIntent : UiAction {
    data class SelectReason(val id: Int) : CreateReportIntent
    data class AdditionalInfoChanged(val text: String) : CreateReportIntent
    data object Submit : CreateReportIntent
    data object Back : CreateReportIntent
    data object RetryReasons : CreateReportIntent
}

sealed interface CreateReportEffect : UiEffect {
    data object NavigateBack : CreateReportEffect
}

