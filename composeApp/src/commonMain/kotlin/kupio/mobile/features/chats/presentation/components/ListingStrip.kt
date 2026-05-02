package kupio.mobile.features.chats.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kupio.mobile.features.chats.domain.model.ListingSummary
import kupio.mobile.features.listings.presentation.components.ListingPreviewStrip

@Composable
fun ListingStrip(
    listing: ListingSummary,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    ListingPreviewStrip(
        title = listing.title,
        imageUrl = listing.imageUrl,
        modifier = modifier,
        trailing = trailing,
    )
}

@Composable
fun ListingPrice(
    priceFormatted: String,
) {
    Text(
        text = priceFormatted,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
