package kupio.mobile.features.listings.presentation.create.components

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
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.SelectedListingImage
import kupio.mobile.features.listings.presentation.create.toErrorMessage
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_photo_add
import mobile.composeapp.generated.resources.create_photo_choose_gallery
import mobile.composeapp.generated.resources.create_photo_choose_gallery_supporting
import mobile.composeapp.generated.resources.create_photo_cover
import mobile.composeapp.generated.resources.create_photo_next
import mobile.composeapp.generated.resources.create_photo_placeholder_subtitle
import mobile.composeapp.generated.resources.create_photo_placeholder_title
import mobile.composeapp.generated.resources.create_photo_previous
import mobile.composeapp.generated.resources.create_photo_remove
import mobile.composeapp.generated.resources.create_photo_selected_count
import mobile.composeapp.generated.resources.create_photo_take
import mobile.composeapp.generated.resources.create_photo_take_supporting
import mobile.composeapp.generated.resources.create_photos
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PhotosSection(
    images: List<SelectedListingImage>,
    imageWarning: CreateError?,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var selectedImageIndex by remember { mutableStateOf(0) }
    val safeSelectedIndex = selectedImageIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))
    val selectedImage = images.getOrNull(safeSelectedIndex)

    FormSection(
        title = stringResource(Res.string.create_photos),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(KupioShapes.Large)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    KupioThemeDefaults.strongBorder,
                    KupioShapes.Large
                )
                .bouncingDimClickable(shape = KupioShapes.Large, onClick = onAdd),
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
                    title = selectedImage?.fileName ?: stringResource(Res.string.create_photo_placeholder_title),
                    subtitle = if (selectedImage == null) {
                        stringResource(Res.string.create_photo_placeholder_subtitle)
                    } else {
                        stringResource(Res.string.create_photo_selected_count, images.size)
                    },
                    icon = if (selectedImage == null) Icons.Outlined.PhotoCamera else Icons.Outlined.ImageIcon,
                )
            }
            if (images.size > 1) {
                PreviewArrow(
                    icon = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(Res.string.create_photo_previous),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    onClick = {
                        selectedImageIndex = if (selectedImageIndex <= 0) images.lastIndex else selectedImageIndex - 1
                    },
                )
                PreviewArrow(
                    icon = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(Res.string.create_photo_next),
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
        imageWarning?.let { ErrorText(it.toErrorMessage()) }
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
            text = stringResource(Res.string.create_photo_add),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.create_photo_take)) },
            supportingContent = { Text(stringResource(Res.string.create_photo_take_supporting)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                )
            },
            modifier = Modifier.bouncingDimClickable(shape = KupioShapes.Medium, onClick = onTakePhoto),
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.create_photo_choose_gallery)) },
            supportingContent = { Text(stringResource(Res.string.create_photo_choose_gallery_supporting)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.ImageIcon,
                    contentDescription = null,
                )
            },
            modifier = Modifier.bouncingDimClickable(shape = KupioShapes.Medium, onClick = onChooseFromGallery),
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
            shape = KupioShapes.Large,
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
                shape = KupioShapes.Medium,
            )
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
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
        shape = KupioShapes.Micro,
    ) {
        Text(
            text = stringResource(Res.string.create_photo_cover),
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
            .clip(KupioShapes.Small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                if (selected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    KupioThemeDefaults.strongBorder
                },
                KupioShapes.Small
            )
            .bouncingDimClickable(shape = KupioShapes.Small, onClick = onClick),
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
                .bouncingDimClickable(shape = KupioShapes.Full, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.create_photo_remove, image.fileName),
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
            .bouncingDimClickable(shape = KupioShapes.Small, onClick = onClick),
        shape = KupioShapes.Small,
        color = Color.Transparent,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.create_photo_add),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
