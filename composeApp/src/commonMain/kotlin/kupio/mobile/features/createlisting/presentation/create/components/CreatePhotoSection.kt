package kupio.mobile.features.createlisting.presentation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.createlisting.domain.model.SelectedListingImage

@Composable
internal fun PhotosSection(
    images: List<SelectedListingImage>,
    imageWarning: String?,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var selectedImageIndex by remember { mutableStateOf(0) }
    val safeSelectedIndex = selectedImageIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))
    val selectedImage = images.getOrNull(safeSelectedIndex)

    FormSection(
        title = "Photos",
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    RoundedCornerShape(16.dp)
                )
                .bouncingDimClickable(shape = RoundedCornerShape(16.dp), onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            if (selectedImage?.previewBitmap != null) {
                ComposeImage(
                    bitmap = selectedImage.previewBitmap,
                    contentDescription = selectedImage.fileName,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                PhotoPlaceholder(
                    title = selectedImage?.fileName ?: "Tap to add a photo",
                    subtitle = if (selectedImage == null) "JPG, PNG or WEBP" else "${images.size} selected",
                    icon = if (selectedImage == null) Icons.Outlined.PhotoCamera else Icons.Outlined.ImageIcon,
                )
            }
            if (images.size > 1) {
                PreviewArrow(
                    icon = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous photo",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    onClick = {
                        selectedImageIndex = if (selectedImageIndex <= 0) images.lastIndex else selectedImageIndex - 1
                    },
                )
                PreviewArrow(
                    icon = Icons.Outlined.ChevronRight,
                    contentDescription = "Next photo",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    onClick = {
                        selectedImageIndex = if (selectedImageIndex >= images.lastIndex) 0 else selectedImageIndex + 1
                    },
                )
            }
            if (safeSelectedIndex == 0 && selectedImage != null) {
                CoverBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp),
        ) {
            items(images, key = { it.id }) { image ->
                ImageTile(
                    image = image,
                    selected = images.indexOf(image) == safeSelectedIndex,
                    onClick = { selectedImageIndex = images.indexOf(image) },
                    onRemove = { onRemove(image.id) },
                )
            }
            item(key = "add") {
                AddImageTile(onClick = onAdd)
            }
        }
        imageWarning?.let { ErrorText(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageSourceSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Add photo",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        ListItem(
            headlineContent = { Text("Take photo") },
            supportingContent = { Text("Use camera") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                )
            },
            modifier = Modifier.bouncingDimClickable(shape = RoundedCornerShape(12.dp), onClick = onTakePhoto),
        )
        ListItem(
            headlineContent = { Text("Choose from gallery") },
            supportingContent = { Text("Select existing images") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.ImageIcon,
                    contentDescription = null,
                )
            },
            modifier = Modifier.bouncingDimClickable(shape = RoundedCornerShape(12.dp), onClick = onChooseFromGallery),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PhotoPlaceholder(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.2).sp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewArrow(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
            )
            .bouncingDimClickable(shape = RoundedCornerShape(14.dp), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CoverBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "COVER",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.surface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ImageTile(
    image: SelectedListingImage,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                    },
                ),
                RoundedCornerShape(10.dp)
            )
            .bouncingDimClickable(shape = RoundedCornerShape(10.dp), onClick = onClick),
    ) {
        if (image.previewBitmap != null) {
            ComposeImage(
                bitmap = image.previewBitmap,
                contentDescription = image.fileName,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ImageIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(16.dp)
                .bouncingDimClickable(shape = RoundedCornerShape(99.dp), onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${image.fileName}",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddImageTile(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .bouncingDimClickable(shape = RoundedCornerShape(10.dp), onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
