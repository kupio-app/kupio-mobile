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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import kupio.mobile.core.designsystem.KupioBottomNav
import kupio.mobile.core.designsystem.KupioBottomNavItem
import kupio.mobile.core.designsystem.KupioSnackbar
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.SnackbarEvent
import kupio.mobile.core.presentation.SnackbarManager
import kupio.mobile.features.chats.data.ConversationsStore
import kupio.mobile.features.listings.presentation.create.CreateScreen
import kupio.mobile.features.main.tabs.ChatsTab
import kupio.mobile.features.main.tabs.HomeTab
import kupio.mobile.features.main.tabs.MeTab
import kupio.mobile.features.main.tabs.SavedTab
import kotlinx.coroutines.delay
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_chats
import mobile.composeapp.generated.resources.nav_home
import mobile.composeapp.generated.resources.nav_me
import mobile.composeapp.generated.resources.nav_saved
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

class MainTabsScreen : Screen {
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        val store = koinInject<ConversationsStore>()
        val snackbarManager = koinInject<SnackbarManager>()
        val totalUnread by store.unreadCount.collectAsStateWithLifecycle()

        var currentEvent by remember { mutableStateOf<SnackbarEvent?>(null) }
        var displayEvent by remember { mutableStateOf<SnackbarEvent?>(null) }

        LaunchedEffect(Unit) { store.refreshUnreadCount() }

        LaunchedEffect(Unit) {
            snackbarManager.events.collect { event ->
                displayEvent = event
                currentEvent = event
            }
        }

        LaunchedEffect(currentEvent) {
            if (currentEvent != null) {
                delay(2500.milliseconds)
                currentEvent = null
            }
        }

        TabNavigator(HomeTab) { tabNavigator ->
            val tabs = listOf(HomeTab, SavedTab, ChatsTab, MeTab)
            val selectedIndex = tabs.indexOfFirst { it == tabNavigator.current }.coerceAtLeast(0)
            val spacing = KupioThemeDefaults.spacing

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

                    if (displayEvent != null) {
                        KupioSnackbar(
                            visible = currentEvent != null,
                            icon = displayEvent!!.icon,
                            message = displayEvent!!.message,
                            actionLabel = displayEvent!!.actionLabel,
                            onAction = {
                                val action = displayEvent!!.onAction
                                currentEvent = null
                                action?.invoke()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = spacing.lg)
                                .padding(bottom = spacing.md),
                        )
                    }
                }
            }
        }
    }
}
