package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun ListingPreviewStrip(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    priceFormatted: String? = null,
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
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            priceFormatted?.takeIf { it.isNotBlank() }?.let { price ->
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            trailing()
        }
    }
}
