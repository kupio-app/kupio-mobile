package kupio.mobile.features.search.presentation.queries

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.search.domain.model.RecentSearch
import kupio.mobile.features.search.presentation.components.SearchTopBar
import kupio.mobile.features.search.presentation.filters.SearchFiltersScreen
import kupio.mobile.features.search.presentation.results.SearchResultsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.search_no_recent
import mobile.composeapp.generated.resources.search_recent_in_category
import mobile.composeapp.generated.resources.search_recent_searches
import mobile.composeapp.generated.resources.search_tap_to_reuse
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class SearchQueriesScreen(val initialQuery: String = "") : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<SearchQueriesViewModel> { parametersOf(initialQuery) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is SearchQueriesEffect.NavigateToResults ->
                    navigator.push(SearchResultsScreen(effect.filters))
                SearchQueriesEffect.NavigateToFilters ->
                    rootNavigator.push(SearchFiltersScreen(openResultsOnApply = true))
                SearchQueriesEffect.NavigateBack -> navigator.pop()
            }
        }

        SearchQueriesContent(state = state, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchQueriesContent(
    state: SearchQueriesState,
    onIntent: (SearchQueriesIntent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SearchTopBar(
            query = state.query,
            onQueryChange = { onIntent(SearchQueriesIntent.QueryChanged(it)) },
            onSubmit = { onIntent(SearchQueriesIntent.Submit) },
            onBack = { onIntent(SearchQueriesIntent.Back) },
            onFiltersClick = { onIntent(SearchQueriesIntent.OpenFilters) },
            modifier = Modifier.focusRequester(focusRequester),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = KupioThemeDefaults.spacing.md),
        ) {
            stickyHeader {
                SectionHeader(
                    title = stringResource(Res.string.search_recent_searches),
                    trailingLabel = stringResource(Res.string.search_tap_to_reuse),
                )
            }

            if (state.recentSearches.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.search_no_recent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = KupioThemeDefaults.spacing.lg,
                            vertical = KupioThemeDefaults.spacing.sm,
                        ),
                    )
                }
            } else {
                items(state.recentSearches, key = { it.query }) { search ->
                    RecentSearchItem(
                        search = search,
                        onTap = { onIntent(SearchQueriesIntent.TapRecent(search)) },
                        onTapChevron = { onIntent(SearchQueriesIntent.TapRecentWithFilters(search)) },
                        onRemove = { onIntent(SearchQueriesIntent.RemoveRecent(search.query)) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = KupioThemeDefaults.spacing.lg + 48.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = KupioThemeDefaults.spacing.lg,
                vertical = KupioThemeDefaults.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.SemiBold,
        )
        if (trailingLabel != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun RecentSearchItem(
    search: RecentSearch,
    onTap: () -> Unit,
    onTapChevron: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(onClick = onTap)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onTapChevron),
            shape = KupioShapes.Medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = KupioThemeDefaults.defaultBorder,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = search.query,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (search.categoryName != null) {
                Text(
                    text = stringResource(Res.string.search_recent_in_category, search.categoryName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .bouncingClickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
