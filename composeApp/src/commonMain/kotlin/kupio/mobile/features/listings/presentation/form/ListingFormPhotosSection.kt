package kupio.mobile.features.listings.presentation.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.listings.presentation.components.ListingFloatingIconButton
import kupio.mobile.features.listings.presentation.components.ListingImage
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.create_photo_add
import mobile.composeapp.generated.resources.create_photo_choose_gallery
import mobile.composeapp.generated.resources.create_photo_choose_gallery_supporting
import mobile.composeapp.generated.resources.create_photo_cover
import mobile.composeapp.generated.resources.create_photo_placeholder_subtitle
import mobile.composeapp.generated.resources.create_photo_placeholder_title
import mobile.composeapp.generated.resources.create_photo_remove
import mobile.composeapp.generated.resources.create_photo_selected_count
import mobile.composeapp.generated.resources.create_photo_take
import mobile.composeapp.generated.resources.create_photo_take_supporting
import mobile.composeapp.generated.resources.listing_detail_photo_count
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val HeroHeight = 330.dp
private val ThumbnailSize = 72.dp
private val ThumbnailSpacing = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListingFormPhotosSection(
    images: List<ListingFormImage>,
    warningText: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val pageCount = images.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()
    val imageIds = remember(images) { images.map { it.id } }
    var selectedImageId by remember { mutableStateOf(images.firstOrNull()?.id) }
    var draggingImageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageIds, selectedImageId) {
        val targetIndex = images.indexOfFirst { it.id == selectedImageId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
        if (targetIndex < 0) {
            selectedImageId = images.firstOrNull()?.id
        }
    }

    LaunchedEffect(pagerState.currentPage, imageIds) {
        selectedImageId = images.getOrNull(pagerState.currentPage)?.id
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ListingFormImagePreview(
                    image = images.getOrNull(page),
                    modifier = Modifier.fillMaxSize(),
                    onAdd = onAdd,
                )
            }

            ListingFloatingIconButton(
                onClick = onBack,
                contentDescription = stringResource(Res.string.back),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = spacing.md, top = spacing.sm),
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    images.forEachIndexed { index, _ ->
                        Surface(
                            modifier = Modifier.size(
                                width = if (index == pagerState.currentPage) 20.dp else 6.dp,
                                height = 6.dp,
                            ),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(
                                alpha = if (index == pagerState.currentPage) 1f else 0.6f,
                            ),
                            content = {},
                        )
                    }
                }
            }

            if (images.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(spacing.md),
                    shape = KupioShapes.Small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.surface,
                        )
                        Text(
                            text = stringResource(
                                Res.string.listing_detail_photo_count,
                                pagerState.currentPage + 1,
                                images.size,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ThumbnailSpacing),
            contentPadding = PaddingValues(
                start = spacing.lg,
                top = spacing.md,
                end = spacing.lg,
                bottom = spacing.sm,
            ),
        ) {
            items(images, key = { it.id }) { image ->
                val index = images.indexOfFirst { it.id == image.id }
                ImageTile(
                    image = image,
                    selected = image.id == selectedImageId,
                    dragging = image.id == draggingImageId,
                    onClick = {
                        selectedImageId = image.id
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index.coerceAtLeast(0))
                        }
                    },
                    onRemove = { onRemove(image.id) },
                    onDragStart = { draggingImageId = image.id },
                    onDragEnd = { draggingImageId = null },
                    onMoveBy = { steps ->
                        val currentIndex = images.indexOfFirst { it.id == image.id }
                        val targetIndex = (currentIndex + steps).coerceIn(0, images.lastIndex)
                        if (currentIndex >= 0 && targetIndex != currentIndex) {
                            onMove(currentIndex, targetIndex)
                        }
                    },
                )
            }
            item(key = "add") {
                AddImageTile(onClick = onAdd)
            }
        }
        warningText?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = spacing.lg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingImageSourceSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.create_photo_add),
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
}

@Composable
private fun ListingFormImagePreview(
    image: ListingFormImage?,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    val previewBitmap = image?.previewBitmap
    val imageUrl = image?.imageUrl
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bouncingDimClickable(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        when {
            previewBitmap != null -> ComposeImage(
                bitmap = previewBitmap,
                contentDescription = image.fileName,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )

            !imageUrl.isNullOrBlank() -> ListingImage(
                imageUrl = imageUrl,
                contentDescription = image.fileName,
                modifier = Modifier.matchParentSize(),
            )

            else -> PhotoPlaceholder(
                title = stringResource(Res.string.create_photo_placeholder_title),
                subtitle = stringResource(Res.string.create_photo_placeholder_subtitle),
                icon = Icons.Outlined.PhotoCamera,
            )
        }
    }
}

@Composable
private fun ImageTile(
    image: ListingFormImage,
    selected: Boolean,
    dragging: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMoveBy: (Int) -> Unit,
) {
    val stepPx = with(LocalDensity.current) { (ThumbnailSize + ThumbnailSpacing).toPx() }
    var dragRemainder by remember(image.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(ThumbnailSize)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = if (dragging) dragRemainder else 0f
                val scale = if (dragging) 1.05f else 1f
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(image.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragRemainder = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        dragRemainder = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragRemainder = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragRemainder += dragAmount.x
                        val steps = (dragRemainder / stepPx).roundToInt()
                        if (steps != 0) {
                            onMoveBy(steps)
                            dragRemainder -= steps * stepPx
                        }
                    },
                )
            }
            .clip(KupioShapes.Small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                if (selected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    KupioThemeDefaults.strongBorder
                },
                KupioShapes.Small,
            )
            .bouncingDimClickable(shape = KupioShapes.Small, onClick = onClick),
    ) {
        ListingTileImage(image = image)
        if (selected) {
            CoverBadge(Modifier.align(Alignment.BottomStart).padding(5.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(18.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = CircleShape,
                )
                .bouncingDimClickable(shape = KupioShapes.Full, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.create_photo_remove, image.fileName),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BoxScope.ListingTileImage(image: ListingFormImage) {
    val previewBitmap = image.previewBitmap
    when {
        previewBitmap != null -> ComposeImage(
            bitmap = previewBitmap,
            contentDescription = image.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        !image.imageUrl.isNullOrBlank() -> ListingImage(
            imageUrl = image.imageUrl,
            contentDescription = image.fileName,
            modifier = Modifier.fillMaxSize(),
        )

        else -> Icon(
            imageVector = Icons.Outlined.ImageIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center),
        )
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
            style = MaterialTheme.typography.titleMedium,
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
private fun CoverBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        shape = KupioShapes.Micro,
    ) {
        Text(
            text = stringResource(Res.string.create_photo_cover),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.surface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AddImageTile(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(ThumbnailSize)
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
