package kupio.mobile.features.listings.presentation.detail

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioUserAvatar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.chats.presentation.thread.ChatThreadScreen
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.presentation.components.ListingImage
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.listing_detail_call
import mobile.composeapp.generated.resources.listing_detail_cancel
import mobile.composeapp.generated.resources.listing_detail_description
import mobile.composeapp.generated.resources.listing_detail_favourite
import mobile.composeapp.generated.resources.listing_detail_image
import mobile.composeapp.generated.resources.listing_detail_message_empty
import mobile.composeapp.generated.resources.listing_detail_message_placeholder
import mobile.composeapp.generated.resources.listing_detail_message_seller
import mobile.composeapp.generated.resources.listing_detail_message_title
import mobile.composeapp.generated.resources.listing_detail_photo_count
import mobile.composeapp.generated.resources.listing_detail_posted
import mobile.composeapp.generated.resources.listing_detail_report
import mobile.composeapp.generated.resources.listing_detail_seen_count
import mobile.composeapp.generated.resources.listing_detail_seller_fallback
import mobile.composeapp.generated.resources.listing_detail_seller_profile
import mobile.composeapp.generated.resources.listing_detail_send
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
                is ListingDetailEffect.OpenChat -> navigator.push(ChatThreadScreen(effect.conversationId))
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
        bottomBar = {
            if (state.listing != null && !state.isLoading) {
                DetailActionBar(state = state, onIntent = onIntent)
            }
        },
    ) { paddingValues ->
        Box(
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

    if (state.isMessageDialogVisible) {
        MessageSellerDialog(state = state, onIntent = onIntent)
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
                onBack = { onIntent(ListingDetailIntent.Back) },
            )
        }
        item { ListingSummarySection(listing) }
        if (listing.customFilters.isNotEmpty()) {
            item { ListingSpecsSection(listing.customFilters) }
        }
        item { ListingDescriptionSection(listing.description) }
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
    }
}

@Composable
private fun ListingHero(
    listing: Listing,
    onBack: () -> Unit,
) {
    var selectedIndex by remember(listing.id, listing.imageUrls) { mutableIntStateOf(0) }
    val imageUrls = listing.imageUrls.ifEmpty {
        listing.primaryImageUrl
            ?.takeIf { it.isNotBlank() }
            ?.let(::listOf)
            .orEmpty()
    }
    val selectedUrl = imageUrls.getOrNull(selectedIndex)?.takeIf { it.isNotBlank() }
    val spacing = KupioThemeDefaults.spacing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
    ) {
        ListingImage(
            imageUrl = selectedUrl,
            contentDescription = stringResource(Res.string.listing_detail_image),
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingIconButton(
                onClick = onBack,
                contentDescription = stringResource(Res.string.back),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
            FloatingIconButton(
                onClick = {},
                contentDescription = stringResource(Res.string.listing_detail_favourite),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
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
                            .size(width = if (index == selectedIndex) 20.dp else 6.dp, height = 6.dp)
                            .bouncingClickable { selectedIndex = index },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(
                            alpha = if (index == selectedIndex) 1f else 0.6f,
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
                            selectedIndex + 1,
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

@Composable
private fun FloatingIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.65f)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
    }
}

@Composable
private fun ListingSummarySection(listing: Listing) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
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
                initials = seller?.initials ?: stringResource(Res.string.listing_detail_seller_fallback).take(2),
                size = 44.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = seller?.displayName ?: stringResource(Res.string.listing_detail_seller_fallback),
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
                onClick = { onIntent(ListingDetailIntent.OpenMessageDialog) },
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
private fun MessageSellerDialog(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isSendingMessage) onIntent(ListingDetailIntent.CloseMessageDialog)
        },
        title = { Text(stringResource(Res.string.listing_detail_message_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
                OutlinedTextField(
                    value = state.messageDraft,
                    onValueChange = { onIntent(ListingDetailIntent.MessageChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.listing_detail_message_placeholder)) },
                    minLines = 3,
                    enabled = !state.isSendingMessage,
                    isError = state.messageError != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.messageError != null) {
                    val emptyMessage = stringResource(Res.string.listing_detail_message_empty)
                    Text(
                        text = state.messageError.ifBlank { emptyMessage },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onIntent(ListingDetailIntent.SendMessage) },
                enabled = !state.isSendingMessage,
            ) {
                Text(stringResource(Res.string.listing_detail_send))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onIntent(ListingDetailIntent.CloseMessageDialog) },
                enabled = !state.isSendingMessage,
            ) {
                Text(stringResource(Res.string.listing_detail_cancel))
            }
        },
    )
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
