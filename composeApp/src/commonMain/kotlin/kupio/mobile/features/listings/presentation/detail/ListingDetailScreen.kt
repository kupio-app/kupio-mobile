package kupio.mobile.features.listings.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.listing_detail_title
import mobile.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class ListingDetailScreen(val listingId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<ListingDetailViewModel> { parametersOf(listingId) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                ListingDetailEffect.NavigateBack -> navigator.pop()
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
    KupioScaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.listing_detail_title),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.back),
                        onClick = { onIntent(ListingDetailIntent.Back) },
                    )
                },
            )
        },
    ) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(KupioThemeDefaults.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md, Alignment.CenterVertically),
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onIntent(ListingDetailIntent.Retry) }) {
                    Text(text = stringResource(Res.string.retry))
                }
            }

            state.listing != null -> ListingDetailBody(listing = state.listing)
        }
    }
}

@Composable
private fun ListingDetailBody(listing: Listing) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = listing.formatPrice(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
