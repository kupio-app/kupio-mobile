package kupio.mobile.features.me.presentation.mylistings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopBarIconAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.me.domain.model.OwnedListingStatus
import kupio.mobile.features.me.presentation.mylistings.components.FilterChipsRow
import kupio.mobile.features.me.presentation.mylistings.components.OwnedListingCard
import kupio.mobile.features.me.presentation.mylistings.components.StatusChangeDialog
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_empty
import mobile.composeapp.generated.resources.my_listings_load_error
import mobile.composeapp.generated.resources.my_listings_search
import mobile.composeapp.generated.resources.my_listings_subtitle
import mobile.composeapp.generated.resources.my_listings_title
import mobile.composeapp.generated.resources.my_listings_confirm_activate_title
import mobile.composeapp.generated.resources.my_listings_confirm_activate_body
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_title
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_body
import mobile.composeapp.generated.resources.my_listings_confirm_action
import mobile.composeapp.generated.resources.my_listings_cancel_action
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
                state.isLoading -> KupioLoadingScreen()
                state.errorMessage != null && state.visibleListings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        KupioErrorRetryRow(
                            message = stringResource(Res.string.my_listings_load_error),
                            onRetry = { onIntent(MyListingsIntent.RetryLoad) },
                        )
                    }
                }
                state.visibleListings.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.my_listings_empty),
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
                                onToggleStatus = { onIntent(MyListingsIntent.ToggleActiveClicked(listing.id)) },
                                isStatusActionEnabled =
                                    state.updatingListingId == null && listing.status.canToggleStatus(),
                            )
                        }
                        item { Spacer(modifier = Modifier.height(KupioThemeDefaults.spacing.xl)) }
                    }
                }
            }
        }

        val confirmation = state.statusChangeConfirmation
        if (confirmation != null) {
            val isActivation = confirmation.targetStatus == OwnedListingStatus.ACTIVE
            StatusChangeDialog(
                title = stringResource(
                    if (isActivation) {
                        Res.string.my_listings_confirm_activate_title
                    } else {
                        Res.string.my_listings_confirm_deactivate_title
                    },
                ),
                message = stringResource(
                    if (isActivation) {
                        Res.string.my_listings_confirm_activate_body
                    } else {
                        Res.string.my_listings_confirm_deactivate_body
                    },
                ),
                confirmLabel = stringResource(Res.string.my_listings_confirm_action),
                dismissLabel = stringResource(Res.string.my_listings_cancel_action),
                onConfirm = { onIntent(MyListingsIntent.ConfirmStatusChange) },
                onDismiss = { onIntent(MyListingsIntent.DismissStatusChange) },
            )
        }
    }
}

private fun OwnedListingStatus.canToggleStatus(): Boolean = when (this) {
    OwnedListingStatus.ACTIVE,
    OwnedListingStatus.INACTIVE,
    OwnedListingStatus.DRAFT,
    -> true
    OwnedListingStatus.PLANNED,
    OwnedListingStatus.SOLD,
    -> false
}
