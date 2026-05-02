package kupio.mobile.features.listings.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.presentation.components.AdvertisementHeadline
import kupio.mobile.features.listings.presentation.components.CategoriesRow
import kupio.mobile.features.listings.presentation.components.HomeTopBar
import kupio.mobile.features.listings.presentation.components.ListingCard
import kupio.mobile.features.listings.presentation.components.SearchWithFilters
import kupio.mobile.features.listings.presentation.components.SectionHeader
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import kupio.mobile.features.search.presentation.SearchScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.home_category_all
import mobile.composeapp.generated.resources.home_recommended_count
import mobile.composeapp.generated.resources.home_recommended_error
import mobile.composeapp.generated.resources.home_recommended_title
import mobile.composeapp.generated.resources.screen_home_body
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class FeedScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<FeedViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is FeedEffect.OpenListing -> rootNavigator.push(ListingDetailScreen(effect.id))
                is FeedEffect.OpenSearch -> navigator.push(SearchScreen(effect.query))
            }
        }
        FeedContent(state = state, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val allLabel = stringResource(Res.string.home_category_all)
    val categoryItems = remember(state.categories, allLabel) {
        buildCategoryItems(state.categories, allLabel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HomeTopBar(
            deliveryLocation = state.deliveryLocation,
            hasUnreadNotifications = state.hasUnreadNotifications,
            onDeliveryClick = { onIntent(FeedIntent.SelectDelivery) },
            onNotificationsClick = { onIntent(FeedIntent.OpenNotifications) },
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(FeedIntent.RefreshFeed) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item(key = "headline") {
                    AdvertisementHeadline()
                }

                item(key = "search") {
                    SearchWithFilters(
                        query = state.searchQuery,
                        onQueryChange = { onIntent(FeedIntent.SearchQueryChanged(it)) },
                        onSubmit = { onIntent(FeedIntent.SubmitSearch) },
                        onFiltersClick = { onIntent(FeedIntent.OpenFilters) },
                    )
                }

                item(key = "categories") {
                    CategoriesRow(
                        items = categoryItems,
                        selectedId = state.selectedCategoryId,
                        isLoading = state.isLoadingCategories,
                        error = state.categoriesError,
                        onSelect = { onIntent(FeedIntent.SelectCategory(it)) },
                        onRetry = { onIntent(FeedIntent.RetryLoadCategories) },
                    )
                }

                item(key = "recommended_header") {
                    SectionHeader(
                        title = stringResource(Res.string.home_recommended_title),
                        trailing = if (state.listings.isNotEmpty()) {
                            stringResource(Res.string.home_recommended_count, state.listings.size)
                        } else {
                            null
                        },
                    )
                }

                when {
                    state.isLoadingListings -> item(key = "listings_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = spacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    state.listingsError != null -> item(key = "listings_error") {
                        KupioErrorRetryRow(
                            message = stringResource(Res.string.home_recommended_error),
                            onRetry = { onIntent(FeedIntent.RetryLoadListings) },
                            modifier = Modifier.padding(vertical = spacing.md),
                        )
                    }

                    state.listings.isEmpty() -> item(key = "listings_empty") {
                        Text(
                            text = stringResource(Res.string.screen_home_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> recommendedGrid(
                        listings = state.listings,
                        favouritedIds = state.favouritedIds,
                        togglingFavouriteIds = state.togglingFavouriteIds,
                        onOpen = { id -> onIntent(FeedIntent.OpenListing(id)) },
                        onToggleFavourite = { id -> onIntent(FeedIntent.ToggleFavourite(id)) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.recommendedGrid(
    listings: List<Listing>,
    favouritedIds: Set<String>,
    togglingFavouriteIds: Set<String>,
    onOpen: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
) {
    items(listings.chunked(2), key = { row -> row.first().id }) { row ->
        val spacing = KupioThemeDefaults.spacing
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            row.forEach { listing ->
                ListingCard(
                    listing = listing,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(listing.id) },
                    isFavourited = listing.id in favouritedIds,
                    onFavouriteClick = if (listing.id in togglingFavouriteIds) null else {
                        { onToggleFavourite(listing.id) }
                    },
                )
            }
            if (row.size < 2) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
