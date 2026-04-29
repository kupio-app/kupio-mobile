package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickableIf
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.formatPrice
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.home_listing_favorite
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavourited: Boolean = false,
    onFavouriteClick: (() -> Unit)? = null,
) {
    val spacing = KupioThemeDefaults.spacing

    KupioCardSurface(
        modifier = modifier.bouncingDimClickable { onClick() },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                ListingImage(
                    imageUrl = listing.primaryImageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                )
                val heartModifier = Modifier
                    .padding(spacing.sm)
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .bouncingClickableIf(onFavouriteClick)

                Surface(
                    modifier = heartModifier,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isFavourited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(Res.string.home_listing_favorite),
                            tint = if (isFavourited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listing.formatPrice(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
