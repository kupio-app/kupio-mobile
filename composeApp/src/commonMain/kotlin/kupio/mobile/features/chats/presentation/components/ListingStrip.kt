package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.chats.domain.model.ListingSummary
import kupio.mobile.features.listings.presentation.components.ListingThumbnail

@Composable
fun ListingStrip(
    listing: ListingSummary,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val spacing = KupioThemeDefaults.spacing

    KupioCardSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListingThumbnail(
                imageUrl = listing.imageUrl,
                contentDescription = listing.title,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = listing.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(spacing.sm))
            trailing()
        }
    }
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
