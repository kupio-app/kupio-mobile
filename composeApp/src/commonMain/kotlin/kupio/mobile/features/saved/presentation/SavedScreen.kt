package kupio.mobile.features.saved.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.presentation.components.ListingImage
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_saved
import mobile.composeapp.generated.resources.saved_empty
import mobile.composeapp.generated.resources.saved_load_error
import mobile.composeapp.generated.resources.saved_remove
import mobile.composeapp.generated.resources.saved_subtitle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val listingPlaceholderColors = listOf(
    Color(0xFFCEB99A),
    Color(0xFFB5A088),
    Color(0xFF9B876F),
    Color(0xFF856E58),
)

class SavedScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = generateSequence(navigator) { it.parent }.last()
        val viewModel = koinViewModel<SavedViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                is SavedEffect.OpenListing -> rootNavigator.push(ListingDetailScreen(effect.listingId))
            }
        }

        SavedContent(state = state, onIntent = viewModel::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedContent(
    state: SavedState,
    onIntent: (SavedIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    Scaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.nav_saved),
                subtitle = if (!state.isLoading && state.listings.isNotEmpty()) {
                    stringResource(Res.string.saved_subtitle, state.listings.size)
                } else {
                    null
                },
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(SavedIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                state.errorMessage != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    KupioErrorRetryRow(
                        message = stringResource(Res.string.saved_load_error),
                        onRetry = { onIntent(SavedIntent.Refresh) },
                    )
                }

                state.listings.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.saved_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(state.listings, key = { it.id }) { listing ->
                        SavedListingRow(
                            listing = listing,
                            isRemoving = listing.id in state.removingIds,
                            onClick = { onIntent(SavedIntent.OpenListing(listing.id)) },
                            onRemove = { onIntent(SavedIntent.RemoveFavourite(listing.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedListingRow(
    listing: Listing,
    isRemoving: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    KupioCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            ListingImage(
                imageUrl = listing.primaryImageUrl,
                contentDescription = listing.title,
                modifier = Modifier
                    .size(80.dp)
                    .aspectRatio(1f),
                shape = KupioShapes.Medium
            )
            Spacer(Modifier.width(spacing.md))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listing.formatPrice(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.width(spacing.sm))

            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .let { if (!isRemoving) it.bouncingClickable { onRemove() } else it },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.saved_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
