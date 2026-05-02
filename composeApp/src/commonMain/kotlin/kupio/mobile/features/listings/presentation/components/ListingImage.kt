package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import kupio.mobile.core.designsystem.KupioShapes

@Composable
fun ListingImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0),
) {
    val shapedModifier = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (imageUrl.isNullOrBlank()) {
        Box(modifier = shapedModifier)
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = shapedModifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun ListingThumbnail(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    ListingImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = KupioShapes.Small,
    )
}
