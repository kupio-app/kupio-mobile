package kupio.mobile.features.chats.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import kupio.mobile.features.chats.domain.model.ListingSummary

private val listingPlaceholderColors = listOf(
    Color(0xFFCEB99A),
    Color(0xFFB5A088),
    Color(0xFF9B876F),
    Color(0xFF856E58),
)

@Composable
fun ListingStrip(
    listing: ListingSummary,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val spacing = KupioThemeDefaults.spacing
    val colorIndex = (listing.placeholderSeed and 0x7FFFFFFF) % listingPlaceholderColors.size
    val bgColor = listingPlaceholderColors[colorIndex]

    KupioCardSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, RoundedCornerShape(8.dp)),
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
