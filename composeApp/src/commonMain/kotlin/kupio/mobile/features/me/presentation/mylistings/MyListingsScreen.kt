package kupio.mobile.features.me.presentation.mylistings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.me.presentation.mylistings.components.FilterChipsRow
import kupio.mobile.features.me.presentation.mylistings.components.OwnedListingCard
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_search
import mobile.composeapp.generated.resources.my_listings_subtitle
import mobile.composeapp.generated.resources.my_listings_title
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class MyListingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<MyListingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                MyListingsEffect.NavigateBack -> navigator.pop()
            }
        }

        MyListingsRoute(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun MyListingsRoute(state: MyListingsState, onIntent: (MyListingsIntent) -> Unit) {
    KupioScaffold (
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.my_listings_title),
                subtitle = stringResource(
                    Res.string.my_listings_subtitle,
                    state.activeCount,
                    state.inactiveCount,
                ),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(MyListingsIntent.BackClicked) },
                    )
                },
                trailingContent = {
                    KupioTopBarIconAction(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.my_listings_search),
                        onClick = {},
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            FilterChipsRow(
                selectedFilter = state.filter,
                onFilterSelected = { onIntent(MyListingsIntent.FilterSelected(it)) },
            )
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.visibleListings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No listings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
                    ) {
                        items(state.visibleListings, key = { it.id }) { listing ->
                            OwnedListingCard(
                                listing = listing,
                                onEdit = { onIntent(MyListingsIntent.EditListing(listing.id)) },
                                onBumpUp = { onIntent(MyListingsIntent.BumpUp(listing.id)) },
                                onPromote = { onIntent(MyListingsIntent.Promote(listing.id)) },
                                onToggleActive = { deactivate ->
                                    onIntent(MyListingsIntent.ToggleActive(listing.id, deactivate))
                                },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(KupioThemeDefaults.spacing.xl)) }
                    }
                }
            }
        }
    }
}
