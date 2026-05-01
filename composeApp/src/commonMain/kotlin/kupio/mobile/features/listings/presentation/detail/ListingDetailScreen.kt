package kupio.mobile.features.listings.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioDefaultButton
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioUserAvatar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.chats.presentation.thread.ChatThreadScreen
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.presentation.components.ListingFloatingIconButton
import kupio.mobile.features.listings.presentation.components.ListingImage
import kupio.mobile.features.listings.presentation.edit.EditListingScreen
import kupio.mobile.features.me.presentation.mylistings.components.StatusChangeDialog
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.listing_detail_chats
import mobile.composeapp.generated.resources.listing_detail_call
import mobile.composeapp.generated.resources.listing_detail_cancel
import mobile.composeapp.generated.resources.listing_detail_description
import mobile.composeapp.generated.resources.listing_detail_favourite
import mobile.composeapp.generated.resources.listing_detail_favourites
import mobile.composeapp.generated.resources.listing_detail_free
import mobile.composeapp.generated.resources.listing_detail_image
import mobile.composeapp.generated.resources.listing_detail_message_empty
import mobile.composeapp.generated.resources.listing_detail_message_placeholder
import mobile.composeapp.generated.resources.listing_detail_message_seller
import mobile.composeapp.generated.resources.listing_detail_message_title
import mobile.composeapp.generated.resources.listing_detail_metrics
import mobile.composeapp.generated.resources.listing_detail_photo_count
import mobile.composeapp.generated.resources.listing_detail_posted
import mobile.composeapp.generated.resources.listing_detail_promoted
import mobile.composeapp.generated.resources.listing_detail_report
import mobile.composeapp.generated.resources.listing_detail_seen_count
import mobile.composeapp.generated.resources.listing_detail_seller_fallback
import mobile.composeapp.generated.resources.listing_detail_seller_profile
import mobile.composeapp.generated.resources.listing_detail_send
import mobile.composeapp.generated.resources.listing_detail_status_error
import mobile.composeapp.generated.resources.listing_detail_tradable
import mobile.composeapp.generated.resources.listing_detail_unfavourite
import mobile.composeapp.generated.resources.listing_detail_views
import mobile.composeapp.generated.resources.my_listings_activate
import mobile.composeapp.generated.resources.my_listings_cancel_action
import mobile.composeapp.generated.resources.my_listings_confirm_action
import mobile.composeapp.generated.resources.my_listings_confirm_activate_body
import mobile.composeapp.generated.resources.my_listings_confirm_activate_title
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_body
import mobile.composeapp.generated.resources.my_listings_confirm_deactivate_title
import mobile.composeapp.generated.resources.my_listings_deactivate
import mobile.composeapp.generated.resources.my_listings_edit
import mobile.composeapp.generated.resources.my_listings_extend
import mobile.composeapp.generated.resources.my_listings_filter_active
import mobile.composeapp.generated.resources.my_listings_filter_draft
import mobile.composeapp.generated.resources.my_listings_filter_inactive
import mobile.composeapp.generated.resources.my_listings_filter_planned
import mobile.composeapp.generated.resources.my_listings_filter_sold
import mobile.composeapp.generated.resources.my_listings_promote
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
                showSeenCount = !state.isOwnListing,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListingHero(
    listing: Listing,
    showFavourite: Boolean,
    isFavourited: Boolean,
    isTogglingFavourite: Boolean,
    onBack: () -> Unit,
    onFavouriteClick: () -> Unit,
) {
    val imageUrls = listing.imageUrls.ifEmpty {
        listing.primaryImageUrl
            ?.takeIf { it.isNotBlank() }
            ?.let(::listOf)
            .orEmpty()
    }
    val pageCount = imageUrls.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val spacing = KupioThemeDefaults.spacing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ListingImage(
                imageUrl = imageUrls.getOrNull(page),
                contentDescription = stringResource(Res.string.listing_detail_image),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListingFloatingIconButton(
                onClick = onBack,
                contentDescription = stringResource(Res.string.back),
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            if (showFavourite) {
                ListingFloatingIconButton(
                    onClick = onFavouriteClick,
                    contentDescription = stringResource(
                        if (isFavourited) Res.string.listing_detail_unfavourite
                        else Res.string.listing_detail_favourite,
                    ),
                    enabled = !isTogglingFavourite,
                ) {
                    Icon(
                        imageVector = if (isFavourited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavourited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                imageUrls.forEachIndexed { index, _ ->
                    Surface(
                        modifier = Modifier
                            .size(width = if (index == pagerState.currentPage) 20.dp else 6.dp, height = 6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(
                            alpha = if (index == pagerState.currentPage) 1f else 0.6f,
                        ),
                        content = {},
                    )
                }
            }
        }

        if (imageUrls.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.md),
                shape = KupioShapes.Small,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.surface,
                    )
                    Text(
                        text = stringResource(
                            Res.string.listing_detail_photo_count,
                            pagerState.currentPage + 1,
                            imageUrls.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListingSummarySection(
    listing: Listing,
    ownerMetadata: ListingOwnerMetadataUi?,
    showStatus: Boolean,
    showSeenCount: Boolean,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = listing.categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "ID ${listing.id.take(8)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showStatus) {
                val status = ownerMetadata?.status ?: listing.status
                StatusChip(
                    text = status.label(),
                    isActive = status == ListingStatus.ACTIVE,
                )
            }
            if (showStatus && ownerMetadata?.isPromoted == true) {
                StatusChip(
                    text = stringResource(Res.string.listing_detail_promoted),
                    isPromoted = true,
                )
            }
            if (listing.isFree) {
                StatusChip(text = stringResource(Res.string.listing_detail_free))
            }
            if (listing.isTradable) {
                StatusChip(text = stringResource(Res.string.listing_detail_tradable))
            }
        }
        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = listing.formatPrice(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (showSeenCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.listing_detail_seen_count, listing.seenCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    isPromoted: Boolean = false,
    isActive: Boolean = false,
) {
    Surface(
        shape = if (isActive) KupioShapes.Full else KupioShapes.Small,
        color = when {
            isActive -> MaterialTheme.colorScheme.primaryContainer
            isPromoted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (isActive) KupioThemeDefaults.spacing.sm else 8.dp,
                vertical = if (isActive) 2.dp else 3.dp,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isPromoted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun OwnerMetricsSection(
    ownerMetadata: ListingOwnerMetadataUi?,
    fallbackSeenCount: Int,
) {
    val spacing = KupioThemeDefaults.spacing
    val metrics = if (ownerMetadata == null) {
        listOf(
            OwnerMetricUi(
                count = fallbackSeenCount,
                label = stringResource(Res.string.listing_detail_views),
                icon = Icons.Outlined.Visibility,
            ),
        )
    } else {
        listOf(
            OwnerMetricUi(
                count = ownerMetadata.seenCount,
                label = stringResource(Res.string.listing_detail_views),
                icon = Icons.Outlined.Visibility,
            ),
            OwnerMetricUi(
                count = ownerMetadata.favouritesCount,
                label = stringResource(Res.string.listing_detail_favourites),
                icon = Icons.Outlined.FavoriteBorder,
            ),
            OwnerMetricUi(
                count = ownerMetadata.chatsCount,
                label = stringResource(Res.string.listing_detail_chats),
                icon = Icons.AutoMirrored.Outlined.Message,
            ),
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionLabel(stringResource(Res.string.listing_detail_metrics))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = KupioShapes.Large,
            color = MaterialTheme.colorScheme.surface,
            border = KupioThemeDefaults.defaultBorder,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                metrics.forEachIndexed { index, metric ->
                    OwnerMetricCell(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                    )
                    if (index < metrics.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(KupioThemeDefaults.borderWidths.thin)
                                .height(108.dp)
                                .background(KupioThemeDefaults.softDividerColor),
                        )
                    }
                }
            }
        }
    }
}

private data class OwnerMetricUi(
    val count: Int,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun OwnerMetricCell(
    metric: OwnerMetricUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = KupioThemeDefaults.spacing.md,
            vertical = KupioThemeDefaults.spacing.md,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = metric.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = metric.count.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = metric.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OwnerStatusError(message: String) {
    val text = message.ifBlank { stringResource(Res.string.listing_detail_status_error) }
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.lg),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ListingSpecsSection(filters: Map<String, String>) {
    val spacing = KupioThemeDefaults.spacing
    val rows = filters.entries.chunked(2)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            rows.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { index, entry ->
                        SpecCell(
                            label = entry.key.replace('_', ' '),
                            value = entry.value,
                            modifier = Modifier.weight(1f),
                        )
                        if (index == 0 && row.size > 1) {
                            Box(
                                modifier = Modifier
                                    .width(KupioThemeDefaults.borderWidths.thin)
                                    .height(54.dp)
                                    .background(KupioThemeDefaults.softDividerColor),
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                if (rowIndex < rows.lastIndex) {
                    HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
                }
            }
        }
    }
}

@Composable
private fun SpecCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = KupioThemeDefaults.spacing.md, vertical = KupioThemeDefaults.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ListingDescriptionSection(description: String) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionLabel(stringResource(Res.string.listing_detail_description))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
    }
}

@Composable
private fun SellerSection(
    seller: ListingSellerUi?,
    onProfile: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val sellerFallback = stringResource(Res.string.listing_detail_seller_fallback)
    val sellerName = seller?.displayName?.takeIf { it.isNotBlank() } ?: sellerFallback
    val sellerInitials = seller?.initials?.takeIf { it.isNotBlank() } ?: sellerFallback.take(2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            KupioUserAvatar(
                initials = sellerInitials,
                size = 44.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = sellerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = KupioShapes.Small,
                color = MaterialTheme.colorScheme.surface,
                border = KupioThemeDefaults.strongBorder,
                modifier = Modifier.bouncingClickable(onClick = onProfile),
            ) {
                Text(
                    text = stringResource(Res.string.listing_detail_seller_profile),
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ListingFooter(
    listing: Listing,
    onReport: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.listing_detail_posted, listing.createdAt.take(10)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.bouncingDimClickable(onClick = onReport),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.listing_detail_report),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DetailActionBar(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val canCall = state.seller?.let { !it.isCallsDisabled && !it.phone.isNullOrBlank() } == true
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onIntent(ListingDetailIntent.CallSeller) },
                enabled = canCall,
                shape = KupioShapes.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                ),
                border = KupioThemeDefaults.strongBorder,
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = 13.dp),
            ) {
                Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.listing_detail_call))
            }
            Button(
                onClick = { onIntent(ListingDetailIntent.OpenMessageSheet) },
                modifier = Modifier.weight(1f),
                shape = KupioShapes.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                contentPadding = PaddingValues(vertical = 13.dp),
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.listing_detail_message_seller),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun OwnerActionBar(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val listing = state.listing ?: return
    val spacing = KupioThemeDefaults.spacing
    val ownerStatus = state.ownerMetadata?.status ?: listing.status
    val canToggle = ownerStatus.canToggleOwnerStatus() && !state.isUpdatingStatus
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OwnerActionButton(
                label = stringResource(Res.string.my_listings_edit),
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.EditListing) },
                modifier = Modifier.weight(1f),
            )
            OwnerActionButton(
                label = if (state.ownerMetadata?.isPromoted == true) {
                    stringResource(Res.string.my_listings_extend)
                } else {
                    stringResource(Res.string.my_listings_promote)
                },
                icon = { Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.PromoteListing) },
                modifier = Modifier.weight(1f),
                emphasis = true,
            )
            OwnerActionButton(
                label = ownerStatus.toggleLabel(),
                icon = { Icon(Icons.Outlined.Circle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { onIntent(ListingDetailIntent.ToggleOwnerStatus) },
                modifier = Modifier.weight(1f),
                enabled = canToggle,
            )
        }
    }
}

@Composable
private fun OwnerActionButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = KupioShapes.Large,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasis) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (emphasis) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        border = if (emphasis) null else KupioThemeDefaults.strongBorder,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 13.dp),
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageSellerSheet(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isSendingMessage) onIntent(ListingDetailIntent.CloseMessageSheet)
        },
        sheetState = sheetState,
        shape = KupioShapes.ExtraLarge,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val emptyMessage = stringResource(Res.string.listing_detail_message_empty)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KupioThemeDefaults.spacing.lg)
                .padding(bottom = KupioThemeDefaults.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            Text(
                text = stringResource(Res.string.listing_detail_message_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            KupioTextField(
                value = state.messageDraft,
                onValueChange = { onIntent(ListingDetailIntent.MessageChanged(it)) },
                label = stringResource(Res.string.listing_detail_message_title),
                placeholder = stringResource(Res.string.listing_detail_message_placeholder),
                error = state.messageError?.ifBlank { emptyMessage },
                singleLine = false,
                minLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onIntent(ListingDetailIntent.CloseMessageSheet) },
                    enabled = !state.isSendingMessage,
                ) {
                    Text(stringResource(Res.string.listing_detail_cancel))
                }
                KupioDefaultButton(
                    text = stringResource(Res.string.listing_detail_send),
                    modifier = Modifier.weight(1f),
                    onClick = { onIntent(ListingDetailIntent.SendMessage) },
                    enabled = !state.isSendingMessage,
                    loading = state.isSendingMessage,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ListingStatus.label(): String = when (this) {
    ListingStatus.ACTIVE -> stringResource(Res.string.my_listings_filter_active)
    ListingStatus.INACTIVE -> stringResource(Res.string.my_listings_filter_inactive)
    ListingStatus.DRAFT -> stringResource(Res.string.my_listings_filter_draft)
    ListingStatus.PLANNED -> stringResource(Res.string.my_listings_filter_planned)
    ListingStatus.SOLD -> stringResource(Res.string.my_listings_filter_sold)
}

@Composable
private fun ListingStatus.toggleLabel(): String = when (this) {
    ListingStatus.ACTIVE -> stringResource(Res.string.my_listings_deactivate)
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> stringResource(Res.string.my_listings_activate)
}

private fun ListingStatus.canToggleOwnerStatus(): Boolean = when (this) {
    ListingStatus.ACTIVE,
    ListingStatus.INACTIVE,
    ListingStatus.DRAFT,
    -> true
    ListingStatus.PLANNED,
    ListingStatus.SOLD,
    -> false
}
