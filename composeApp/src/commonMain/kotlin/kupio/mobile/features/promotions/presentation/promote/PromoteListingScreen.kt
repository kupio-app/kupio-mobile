package kupio.mobile.features.promotions.presentation.promote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kupio.mobile.core.designsystem.KupioDefaultButton
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.borderTop
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.formatPrice
import kupio.mobile.features.listings.presentation.components.ListingPreviewStrip
import kupio.mobile.features.promotions.domain.model.PromotionPacket
import kupio.mobile.features.promotions.domain.model.PromotionType
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.promotion_choose_packet
import mobile.composeapp.generated.resources.promotion_done
import mobile.composeapp.generated.resources.promotion_empty_packets
import mobile.composeapp.generated.resources.promotion_empty_packets_body
import mobile.composeapp.generated.resources.promotion_load_error
import mobile.composeapp.generated.resources.promotion_packet_days
import mobile.composeapp.generated.resources.promotion_packet_one_day
import mobile.composeapp.generated.resources.promotion_packet_price
import mobile.composeapp.generated.resources.promotion_pick_body
import mobile.composeapp.generated.resources.promotion_pick_title
import mobile.composeapp.generated.resources.promotion_submit_with_packet
import mobile.composeapp.generated.resources.promotion_success_body
import mobile.composeapp.generated.resources.promotion_success_title
import mobile.composeapp.generated.resources.promotion_title
import mobile.composeapp.generated.resources.promotion_type_highlight
import mobile.composeapp.generated.resources.promotion_type_top
import mobile.composeapp.generated.resources.promotion_type_unknown
import mobile.composeapp.generated.resources.promotion_type_urgent
import mobile.composeapp.generated.resources.promotion_type_vip
import mobile.composeapp.generated.resources.topbar_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class PromoteListingScreen(val listingId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<PromoteListingViewModel>(
            key = "promote-listing-$listingId",
        ) { parametersOf(listingId) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                PromoteListingEffect.NavigateBack -> navigator.pop()
            }
        }

        PromoteListingRoute(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun PromoteListingRoute(
    state: PromoteListingState,
    onIntent: (PromoteListingIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.promotion_title),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(PromoteListingIntent.BackClicked) },
                    )
                },
            )
        },
        bottomBar = {
            if (!state.isLoading && state.errorMessage == null && state.listing != null) {
                PromoteBottomBar(state = state, onIntent = onIntent)
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
                        message = state.errorMessage.ifBlank {
                            stringResource(Res.string.promotion_load_error)
                        },
                        onRetry = { onIntent(PromoteListingIntent.RetryLoad) },
                    )
                }
                state.listing != null -> PromoteListingBody(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun PromoteListingBody(
    state: PromoteListingState,
    onIntent: (PromoteListingIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val listing = state.listing ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.md,
            end = spacing.md,
            top = spacing.md,
            bottom = spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            ListingPreviewStrip(
                title = listing.title,
                imageUrl = listing.primaryImageUrl,
                priceFormatted = listing.formatPrice(),
            )
        }

        if (state.createdPromotion != null) {
            item {
                PromotionSuccessCard(packet = state.createdPromotion.packet)
            }
        } else {
            item { PromotionIntro() }

            if (state.packets.isEmpty()) {
                item { EmptyPacketsCard() }
            } else {
                items(state.packets, key = { it.id }) { packet ->
                    PromotionPacketCard(
                        packet = packet,
                        selected = state.selectedPacketId == packet.id,
                        onClick = { onIntent(PromoteListingIntent.PacketSelected(packet.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PromotionIntro() {
    Column(
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.promotion_pick_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.promotion_pick_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PromotionPacketCard(
    packet: PromotionPacket,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (selected) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconContainer = if (selected) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val chipContainer = if (selected) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val border = if (selected) {
        BorderStroke(KupioThemeDefaults.borderWidths.regular, MaterialTheme.colorScheme.onSurface)
    } else {
        KupioThemeDefaults.defaultBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable(onClick = onClick),
        color = containerColor,
        shape = KupioShapes.Large,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = KupioShapes.Medium,
                    color = iconContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) contentColor else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packet.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = packet.type.label(),
                                style = MaterialTheme.typography.labelMedium,
                                color = supportingColor,
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            text = stringResource(Res.string.promotion_packet_price, packet.price),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor,
                        )
                    }
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor,
                    )
                }
            }

            Text(
                text = packet.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.promotion_pick_body),
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                PacketMetaChip(
                    text = packet.durationLabel(),
                    selected = selected,
                    containerColor = chipContainer,
                    contentColor = contentColor,
                )
                PacketMetaChip(
                    text = packet.type.label(),
                    selected = selected,
                    containerColor = chipContainer,
                    contentColor = contentColor,
                )
            }
        }
    }
}

@Composable
private fun PacketMetaChip(
    text: String,
    selected: Boolean,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = KupioShapes.Small,
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.sm, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (selected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PromotionSuccessCard(packet: PromotionPacket) {
    KupioCardSurface {
        Column(
            modifier = Modifier.padding(KupioThemeDefaults.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = KupioShapes.Small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.promotion_success_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = packet.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider(color = KupioThemeDefaults.softDividerColor)
            Text(
                text = stringResource(Res.string.promotion_success_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyPacketsCard() {
    KupioCardSurface {
        Column(
            modifier = Modifier.padding(KupioThemeDefaults.spacing.md),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.promotion_empty_packets),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.promotion_empty_packets_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PromoteBottomBar(
    state: PromoteListingState,
    onIntent: (PromoteListingIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val selectedPacket = state.selectedPacket
    val success = state.createdPromotion != null
    val buttonLabel = when {
        success -> stringResource(Res.string.promotion_done)
        selectedPacket != null -> stringResource(Res.string.promotion_submit_with_packet, selectedPacket.name)
        else -> stringResource(Res.string.promotion_choose_packet)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .borderTop(KupioThemeDefaults.borderWidths.thin, KupioThemeDefaults.softDividerColor),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            state.submitError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            KupioDefaultButton(
                text = buttonLabel,
                onClick = {
                    onIntent(
                        if (success) {
                            PromoteListingIntent.DoneClicked
                        } else {
                            PromoteListingIntent.PromoteClicked
                        },
                    )
                },
                enabled = success || state.canSubmit,
                loading = state.isSubmitting,
            )
        }
    }
}

@Composable
private fun PromotionPacket.durationLabel(): String =
    if (durationDays == 1) {
        stringResource(Res.string.promotion_packet_one_day)
    } else {
        stringResource(Res.string.promotion_packet_days, durationDays)
    }

@Composable
private fun PromotionType.label(): String = when (this) {
    PromotionType.TOP -> stringResource(Res.string.promotion_type_top)
    PromotionType.HIGHLIGHT -> stringResource(Res.string.promotion_type_highlight)
    PromotionType.URGENT -> stringResource(Res.string.promotion_type_urgent)
    PromotionType.VIP -> stringResource(Res.string.promotion_type_vip)
    PromotionType.UNKNOWN -> stringResource(Res.string.promotion_type_unknown)
}
