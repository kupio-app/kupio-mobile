package kupio.mobile.features.reports.presentation.moderator

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.features.reports.domain.model.ReportDetail

data class ModeratorReportDetailState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitError: ModeratorReportDetailSubmitError? = null,
    val report: ReportDetail? = null,
    val sellerProfile: ReportSellerProfileUi? = null,
    val selectedDecision: ReportDecision? = null,
    val moderatorComment: String = "",
) : UiState

data class ReportSellerProfileUi(
    val displayName: String?,
    val username: String?,
    val createdAt: String?,
)

fun ModeratorReportDetailState.sellerDisplayName(): String {
    val report = report ?: return ""
    return sellerProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: report.sellerDisplayName.takeIf { it.isNotBlank() }
        ?: sellerProfile?.username?.takeIf { it.isNotBlank() }
        ?: report.sellerUsername
}

enum class ReportDecision {
    DECLINE, REMOVE_LISTING, BAN_USER;

    fun toApiValue(): String = when (this) {
        DECLINE -> "decline"
        REMOVE_LISTING -> "remove_listing"
        BAN_USER -> "ban_user"
    }
}

enum class ModeratorReportDetailSubmitError {
    SELECT_DECISION,
    SUBMIT_FAILED,
}

sealed interface ModeratorReportDetailIntent : UiAction {
    data object BackClicked : ModeratorReportDetailIntent
    data object Retry : ModeratorReportDetailIntent
    data class DecisionSelected(val decision: ReportDecision) : ModeratorReportDetailIntent
    data class CommentChanged(val text: String) : ModeratorReportDetailIntent
    data object SubmitDecision : ModeratorReportDetailIntent
}

sealed interface ModeratorReportDetailEffect : UiEffect {
    data object NavigateBack : ModeratorReportDetailEffect
    data object ShowSuccess : ModeratorReportDetailEffect
}
