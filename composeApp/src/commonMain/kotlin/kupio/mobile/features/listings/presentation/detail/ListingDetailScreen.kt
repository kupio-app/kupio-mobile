package kupio.mobile.features.listings.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioSnackbar
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.core.presentation.SnackbarEvent
import kupio.mobile.core.presentation.SnackbarManager
import kotlinx.coroutines.delay
import kupio.mobile.features.chats.presentation.thread.ChatThreadScreen
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.presentation.detail.components.DetailActionBar
import kupio.mobile.features.listings.presentation.detail.components.ListingDescriptionSection
import kupio.mobile.features.listings.presentation.detail.components.ListingFooter
import kupio.mobile.features.listings.presentation.detail.components.ListingHero
import kupio.mobile.features.listings.presentation.detail.components.ListingSpecsSection
import kupio.mobile.features.listings.presentation.detail.components.ListingSummarySection
import kupio.mobile.features.listings.presentation.detail.components.MessageSellerSheet
import kupio.mobile.features.listings.presentation.detail.components.OwnerActionBar
import kupio.mobile.features.listings.presentation.detail.components.OwnerMetricsSection
import kupio.mobile.features.listings.presentation.detail.components.OwnerStatusError
import kupio.mobile.features.listings.presentation.detail.components.SellerSection
import kupio.mobile.features.listings.presentation.edit.EditListingScreen
import kupio.mobile.features.me.presentation.mylistings.components.StatusChangeDialog
import kupio.mobile.features.reports.presentation.create.CreateReportScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.my_listings_activate
import mobile.composeapp.generated.resources.my_listings_cancel_action
import mobile.composeapp.generated.resources.my_listings_confirm_action
import mobile.composeapp.generated.resources.my_listings_confirm_activate_body
import mobile.composeapp.generated.resources.my_listings_confirm_activate_title
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_body
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_title
import mobile.composeapp.generated.resources.my_listings_deactivate
import mobile.composeapp.generated.resources.my_listings_filter_active
import mobile.composeapp.generated.resources.my_listings_filter_draft
import mobile.composeapp.generated.resources.my_listings_filter_inactive
import mobile.composeapp.generated.resources.my_listings_filter_planned
import mobile.composeapp.generated.resources.my_listings_filter_sold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds

data class ListingDetailScreen(val listingId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ListingDetailViewModel>(
            key = "listing-detail-$listingId",
        ) { parametersOf(listingId) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                ListingDetailEffect.NavigateBack -> navigator.pop()
                is ListingDetailEffect.OpenChat -> navigator.push(ChatThreadScreen(effect.conversationId))
                is ListingDetailEffect.OpenEdit -> navigator.replace(EditListingScreen(effect.listingId))
                is ListingDetailEffect.NavigateToReport -> navigator.push(
                    CreateReportScreen(
                        listingId = effect.listingId,
                        listingTitle = effect.listingTitle,
                        listingImageUrl = effect.listingImageUrl,
                        listingPriceFormatted = effect.listingPriceFormatted,
                    )
                )
            }
        }
        ListingDetailContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun ListingDetailContent(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val snackbarManager = koinInject<SnackbarManager>()
    var currentEvent by remember { mutableStateOf<SnackbarEvent?>(null) }
    var displayEvent by remember { mutableStateOf<SnackbarEvent?>(null) }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (state.listing != null && !state.isLoading) {
                if (state.isOwnListing) {
                    OwnerActionBar(state = state, onIntent = onIntent)
                } else {
                    DetailActionBar(state = state, onIntent = onIntent)
                }
            }
        },
    ) { paddingValues ->
        Box {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(ListingDetailIntent.RefreshListing) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                when {
                    state.isLoading -> KupioLoadingScreen()
                    state.errorMessage != null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        KupioErrorRetryRow(
                            message = state.errorMessage,
                            onRetry = { onIntent(ListingDetailIntent.Retry) },
                        )
                    }
                    state.listing != null -> DetailBody(state = state, onIntent = onIntent)
                }
            }

            val spacing = KupioThemeDefaults.spacing
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
                        .padding(
                            start = spacing.lg,
                            end = spacing.lg,
                            bottom = paddingValues.calculateBottomPadding()
                        )
                        .navigationBarsPadding(),
                )
            }
        }
    }

    if (state.isMessageSheetVisible) {
        MessageSellerSheet(state = state, onIntent = onIntent)
    }

    val targetStatus = state.statusChangeTarget
    if (targetStatus != null) {
        val isActivation = targetStatus == ListingStatus.ACTIVE
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
            onConfirm = { onIntent(ListingDetailIntent.ConfirmOwnerStatusChange) },
            onDismiss = { onIntent(ListingDetailIntent.DismissOwnerStatusChange) },
        )
    }
}

@Composable
private fun DetailBody(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val listing = state.listing ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = KupioThemeDefaults.spacing.lg),
    ) {
        item {
            ListingHero(
                listing = listing,
                showFavourite = !state.isOwnListing,
                isFavourited = state.isFavourited,
                isTogglingFavourite = state.isTogglingFavourite,
                onBack = { onIntent(ListingDetailIntent.Back) },
                onFavouriteClick = { onIntent(ListingDetailIntent.ToggleFavourite) },
            )
        }
        item {
            ListingSummarySection(
                listing = listing,
                ownerMetadata = state.ownerMetadata,
                showStatus = state.isOwnListing,
                showPostedAt = !state.isOwnListing,
            )
        }
        if (state.isOwnListing) {
            item {
                OwnerMetricsSection(
                    ownerMetadata = state.ownerMetadata,
                    fallbackSeenCount = listing.seenCount,
                )
            }
        }
        if (listing.customFilters.isNotEmpty()) {
            item { ListingSpecsSection(listing.customFilters) }
        }
        item { ListingDescriptionSection(listing.description) }
        if (!state.isOwnListing) {
            item {
                SellerSection(
                    seller = state.seller,
                    onProfile = { onIntent(ListingDetailIntent.OpenSellerProfile) },
                )
            }
            item {
                ListingFooter(
                    listing = listing,
                    onReport = { onIntent(ListingDetailIntent.ReportListing) },
                )
            }
        } else if (state.statusError != null) {
            item { OwnerStatusError(message = state.statusError) }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun ListingStatus.label(): String = when (this) {
    ListingStatus.ACTIVE -> stringResource(Res.string.my_listings_filter_active)
    ListingStatus.INACTIVE -> stringResource(Res.string.my_listings_filter_inactive)
    ListingStatus.DRAFT -> stringResource(Res.string.my_listings_filter_draft)
    ListingStatus.PLANNED -> stringResource(Res.string.my_listings_filter_planned)
    ListingStatus.SOLD -> stringResource(Res.string.my_listings_filter_sold)
}

@Composable
internal fun ListingStatus.toggleLabel(): String = when (this) {
    ListingStatus.ACTIVE -> stringResource(Res.string.my_listings_deactivate)
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> stringResource(Res.string.my_listings_activate)
}

internal fun ListingStatus.canToggleOwnerStatus(): Boolean = when (this) {
    ListingStatus.ACTIVE,
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    -> true
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> false
}
