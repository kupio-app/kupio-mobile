package kupio.mobile.features.me.presentation.profile

import kupio.mobile.core.presentation.UiAction
import kupio.mobile.core.presentation.UiEffect
import kupio.mobile.core.presentation.UiState
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import kupio.mobile.features.me.domain.model.UserListingStats

data class MeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val user: AuthenticatedUser? = null,
    val stats: UserListingStats? = null,
    val isLoadingStats: Boolean = false,
) : UiState

sealed interface MeIntent : UiAction {
    data object ThemeToggleClicked : MeIntent
    data object MyListingsClicked : MeIntent
    data object ChatsClicked : MeIntent
    data object FavouritesClicked : MeIntent
    data object TopUpBalanceClicked : MeIntent
    data object PaymentsHistoryClicked : MeIntent
    data object PromotionsPackagesClicked : MeIntent
    data object EditProfileClicked : MeIntent
    data object SettingsClicked : MeIntent
    data object ReportsDashboardClicked : MeIntent
}

sealed interface MeEffect : UiEffect {
    data object NavigateToMyListings : MeEffect
    data object NavigateToSettings : MeEffect
    data object NavigateToChats : MeEffect
    data object NavigateToFavourites : MeEffect
}
