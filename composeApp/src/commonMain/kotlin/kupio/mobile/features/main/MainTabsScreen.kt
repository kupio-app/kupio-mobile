package kupio.mobile.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kupio.mobile.core.designsystem.KupioBottomNav
import kupio.mobile.core.designsystem.KupioBottomNavItem
import kupio.mobile.features.chats.data.ConversationsStore
import kupio.mobile.features.listings.presentation.create.CreateScreen
import kupio.mobile.features.main.tabs.ChatsTab
import kupio.mobile.features.main.tabs.HomeTab
import kupio.mobile.features.main.tabs.MeTab
import kupio.mobile.features.main.tabs.SavedTab
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_chats
import mobile.composeapp.generated.resources.nav_home
import mobile.composeapp.generated.resources.nav_me
import mobile.composeapp.generated.resources.nav_saved
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

class MainTabsScreen : Screen {
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        val store = koinInject<ConversationsStore>()
        val totalUnread by store.unreadCount.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) { store.refreshUnreadCount() }

        TabNavigator(HomeTab) { tabNavigator ->
            val tabs = listOf(HomeTab, SavedTab, ChatsTab, MeTab)
            val selectedIndex = tabs.indexOfFirst { it == tabNavigator.current }.coerceAtLeast(0)

            val navItems = listOf(
                KupioBottomNavItem(
                    label = stringResource(Res.string.nav_home),
                    icon = Icons.Outlined.Home,
                    selectedIcon = Icons.Outlined.Home,
                ),
                KupioBottomNavItem(
                    label = stringResource(Res.string.nav_saved),
                    icon = Icons.Default.FavoriteBorder,
                    selectedIcon = Icons.Default.FavoriteBorder,
                ),
                KupioBottomNavItem(
                    label = stringResource(Res.string.nav_chats),
                    icon = Icons.Default.ChatBubbleOutline,
                    selectedIcon = Icons.Default.ChatBubbleOutline,
                    badgeCount = totalUnread.takeIf { it > 0 },
                ),
                KupioBottomNavItem(
                    label = stringResource(Res.string.nav_me),
                    icon = Icons.Default.PersonOutline,
                    selectedIcon = Icons.Default.PersonOutline,
                ),
            )

            Scaffold(
                bottomBar = {
                    KupioBottomNav(
                        items = navItems,
                        selectedIndex = selectedIndex,
                        onItemSelected = { index -> tabNavigator.current = tabs[index] },
                        onCenterActionClick = { rootNavigator.push(CreateScreen()) },
                    )
                },
            ) { paddingValues ->
                Box(Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                    CurrentTab()
                }
            }
        }
    }
}
