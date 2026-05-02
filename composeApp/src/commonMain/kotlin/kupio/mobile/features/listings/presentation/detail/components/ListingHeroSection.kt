package kupio.mobile.features.listings.presentation.detail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.presentation.components.ImageCountBadge
import kupio.mobile.features.listings.presentation.components.ListingFloatingIconButton
import kupio.mobile.features.listings.presentation.components.ListingImage
import kupio.mobile.features.listings.presentation.components.PagerDotsIndicator
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.listing_detail_favourite
import mobile.composeapp.generated.resources.listing_detail_image
import mobile.composeapp.generated.resources.listing_detail_unfavourite
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ListingHero(
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
            PagerDotsIndicator(
                pageCount = imageUrls.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = spacing.md),
            )
        }
        if (imageUrls.isNotEmpty()) {
            ImageCountBadge(
                currentPage = pagerState.currentPage,
                totalCount = imageUrls.size,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.md),
            )
        }
    }
}
