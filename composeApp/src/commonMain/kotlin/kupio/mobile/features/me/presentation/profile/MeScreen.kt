package kupio.mobile.features.me.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.preferences.ThemeMode
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.main.tabs.ChatsTab
import kupio.mobile.features.main.tabs.SavedTab
import kupio.mobile.features.me.presentation.mylistings.MyListingsScreen
import kupio.mobile.features.me.presentation.profile.components.ActivitySection
import kupio.mobile.features.me.presentation.profile.components.BalanceCard
import kupio.mobile.features.me.presentation.profile.components.MyListingsSection
import kupio.mobile.features.me.presentation.profile.components.PaymentsSection
import kupio.mobile.features.me.presentation.profile.components.ProfileSection
import kupio.mobile.features.me.presentation.profile.components.ReportsDashboardCard
import kupio.mobile.features.me.presentation.profile.components.UserInfoSection
import kupio.mobile.features.settings.SettingsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.topbar_profile_title
import mobile.composeapp.generated.resources.topbar_theme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class MeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<MeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                MeEffect.NavigateToMyListings -> rootNavigator.push(MyListingsScreen())
                MeEffect.NavigateToSettings -> rootNavigator.push(SettingsScreen())
                MeEffect.NavigateToChats -> tabNavigator.current = ChatsTab
                MeEffect.NavigateToFavourites -> tabNavigator.current = SavedTab
            }
        }

        MeRoute(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun MeRoute(state: MeState, onIntent: (MeIntent) -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    val themeToggleIcon = when (state.themeMode) {
        ThemeMode.DARK -> Icons.Outlined.LightMode
        ThemeMode.LIGHT, ThemeMode.SYSTEM -> Icons.Outlined.DarkMode
    }

    Scaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.topbar_profile_title),
                trailingContent = {
                    KupioTopBarIconAction(
                        imageVector = themeToggleIcon,
                        contentDescription = stringResource(Res.string.topbar_theme),
                        onClick = { onIntent(MeIntent.ThemeToggleClicked) },
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = spacing.md,
                bottom = paddingValues.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item { UserInfoSection(user = state.user) }
            item { BalanceCard(user = state.user, onTopUp = { onIntent(MeIntent.TopUpBalanceClicked) }) }
            item {
                MyListingsSection(
                    stats = state.stats,
                    onManageClick = { onIntent(MeIntent.MyListingsClicked) },
                )
            }
            item {
                ActivitySection(
                    stats = state.stats,
                    onChatsClick = { onIntent(MeIntent.ChatsClicked) },
                    onFavouritesClick = { onIntent(MeIntent.FavouritesClicked) },
                )
            }
            item {
                PaymentsSection(
                    onTopUpClick = { onIntent(MeIntent.TopUpBalanceClicked) },
                    onPaymentsHistoryClick = { onIntent(MeIntent.PaymentsHistoryClicked) },
                    onPromotionsClick = { onIntent(MeIntent.PromotionsPackagesClicked) },
                )
            }
            item {
                ProfileSection(
                    onEditProfileClick = { onIntent(MeIntent.EditProfileClicked) },
                    onSettingsClick = { onIntent(MeIntent.SettingsClicked) },
                )
            }
            val user = state.user
            if (user != null && (user.role == "moderator" || user.role == "admin")) {
                item {
                    ReportsDashboardCard(
                        openCount = state.stats?.chatsCount ?: 0,
                        onClick = { onIntent(MeIntent.ReportsDashboardClicked) },
                    )
                }
            }
        }
    }
}
