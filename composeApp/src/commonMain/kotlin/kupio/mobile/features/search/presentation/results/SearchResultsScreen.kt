package kupio.mobile.features.search.presentation.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.presentation.components.ListingCard
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchFilters
import kupio.mobile.features.search.domain.model.SearchSortBy
import kupio.mobile.features.search.presentation.components.SearchTopBar
import kupio.mobile.features.search.presentation.filters.SearchFiltersScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_filter_chip_price
import mobile.composeapp.generated.resources.search_filters_deal_for_sale
import mobile.composeapp.generated.resources.search_filters_deal_free
import mobile.composeapp.generated.resources.search_filters_deal_trade
import mobile.composeapp.generated.resources.search_filters_only_with_photos
import mobile.composeapp.generated.resources.search_results_count
import mobile.composeapp.generated.resources.search_results_empty
import mobile.composeapp.generated.resources.search_results_error
import mobile.composeapp.generated.resources.search_results_sort_new
import mobile.composeapp.generated.resources.search_results_sort_top
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class SearchResultsScreen(val initialFilters: SearchFilters) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<SearchResultsViewModel> { parametersOf(initialFilters) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is SearchResultsEffect.NavigateToListing ->
                    rootNavigator.push(ListingDetailScreen(effect.id))
                SearchResultsEffect.NavigateToFilters ->
                    rootNavigator.push(SearchFiltersScreen(openResultsOnApply = false))
                SearchResultsEffect.NavigateBack -> navigator.pop()
            }
        }

        SearchResultsContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun SearchResultsContent(
    state: SearchResultsState,
    onIntent: (SearchResultsIntent) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val spacing = KupioThemeDefaults.spacing

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = gridState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }.collect { if (it) onIntent(SearchResultsIntent.LoadMore) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SearchTopBar(
            query = state.filters.query,
            onQueryChange = { onIntent(SearchResultsIntent.QueryChanged(it)) },
            onSubmit = { onIntent(SearchResultsIntent.Submit) },
            onBack = { onIntent(SearchResultsIntent.Back) },
            onFiltersClick = { onIntent(SearchResultsIntent.OpenFilters) },
        )

        val activeChips = buildActiveFilterChips(state.filters)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(
                start = spacing.lg,
                end = spacing.lg,
                top = spacing.md,
                bottom = spacing.lg,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ResultsHeaderRow(
                    count = state.listings.size,
                    activeSortBy = state.activeSortBy,
                    onSortSelected = { onIntent(SearchResultsIntent.SelectSortBy(it)) },
                )
            }

            if (activeChips.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterChipsRow(
                        chips = activeChips,
                        onRemove = { onIntent(SearchResultsIntent.RemoveFilter(it)) },
                    )
                }
            }

            when {
                state.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
                state.error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    KupioErrorRetryRow(
                        message = stringResource(Res.string.search_results_error),
                        onRetry = { onIntent(SearchResultsIntent.Retry) },
                    )
                }
                state.listings.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.search_results_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> items(state.listings, key = { it.id }) { listing ->
                    ListingCard(
                        listing = listing,
                        onClick = { onIntent(SearchResultsIntent.OpenListing(listing.id)) },
                        isFavourited = listing.id in state.favouritedIds,
                        onFavouriteClick = { onIntent(SearchResultsIntent.ToggleFavourite(listing.id)) },
                    )
                }
            }

            if (state.isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsHeaderRow(
    count: Int,
    activeSortBy: SearchSortBy,
    onSortSelected: (SearchSortBy) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.search_results_count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        SortChipSwitcher(activeSortBy = activeSortBy, onSortSelected = onSortSelected)
    }
}

@Composable
private fun SortChipSwitcher(
    activeSortBy: SearchSortBy,
    onSortSelected: (SearchSortBy) -> Unit,
) {
    val priceSortActive = activeSortBy == SearchSortBy.PRICE_LOW_HIGH || activeSortBy == SearchSortBy.PRICE_HIGH_LOW
    val priceLabel = if (activeSortBy == SearchSortBy.PRICE_HIGH_LOW) "€↓" else "€↑"

    Surface(
        shape = KupioShapes.Full,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(modifier = Modifier.height(30.dp)) {
            SortSegment(
                label = stringResource(Res.string.search_results_sort_top),
                selected = activeSortBy == SearchSortBy.RECOMMENDED,
                onClick = { onSortSelected(SearchSortBy.RECOMMENDED) },
            )
            SortSegmentDivider()
            SortSegment(
                label = stringResource(Res.string.search_results_sort_new),
                selected = activeSortBy == SearchSortBy.NEWEST_FIRST,
                onClick = { onSortSelected(SearchSortBy.NEWEST_FIRST) },
            )
            SortSegmentDivider()
            SortSegment(
                label = priceLabel,
                selected = priceSortActive,
                onClick = {
                    val next = if (activeSortBy == SearchSortBy.PRICE_LOW_HIGH) SearchSortBy.PRICE_HIGH_LOW else SearchSortBy.PRICE_LOW_HIGH
                    onSortSelected(next)
                },
            )
        }
    }
}

@Composable
private fun SortSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (selected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent)
            .bouncingClickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SortSegmentDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    )
}

@Composable
private fun FilterChipsRow(
    chips: List<Pair<String, FilterKey>>,
    onRemove: (FilterKey) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        Spacer(Modifier.width(0.dp))
        chips.forEach { (label, key) ->
            DismissibleFilterChip(
                label = label,
                onDismiss = { onRemove(key) },
            )
        }
    }
}

@Composable
private fun DismissibleFilterChip(
    label: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(34.dp)
            .bouncingDimClickable(shape = KupioShapes.Full, onClick = onDismiss),
        shape = KupioShapes.Full,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun buildActiveFilterChips(filters: SearchFilters): List<Pair<String, FilterKey>> {
    val chips = mutableListOf<Pair<String, FilterKey>>()
    if (filters.categoryName != null) {
        chips += filters.categoryName to FilterKey.Category
    }
    if (filters.minPrice != null || filters.maxPrice != null) {
        val min = filters.minPrice ?: 0
        val max = filters.maxPrice ?: 9999
        chips += stringResource(Res.string.search_filter_chip_price, min, max) to FilterKey.Price
    }
    when (filters.dealType) {
        DealType.FOR_SALE -> chips += stringResource(Res.string.search_filters_deal_for_sale) to FilterKey.DealType
        DealType.FREE -> chips += stringResource(Res.string.search_filters_deal_free) to FilterKey.DealType
        DealType.TRADE -> chips += stringResource(Res.string.search_filters_deal_trade) to FilterKey.DealType
        DealType.ANY -> Unit
    }
    if (filters.onlyWithPhotos) {
        chips += stringResource(Res.string.search_filters_only_with_photos) to FilterKey.OnlyWithPhotos
    }
    filters.customFilters.forEach { (slug, value) ->
        chips += value to FilterKey.Custom(slug)
    }
    return chips
}
