package kupio.mobile.features.createlisting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.extensions.loadImageBitmap
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import kotlin.random.Random
import kupio.mobile.core.designsystem.KupioPrimaryButton
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.createlisting.domain.model.SelectedListingImage
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.topbar_back
import mobile.composeapp.generated.resources.topbar_new_listing_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MaxListingImages = 8

class CreateScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<CreateViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                CreateEffect.NavigateBack -> navigator.pop()
            }
        }

        CreateContent(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateContent(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    val imagePicker = rememberImagePickerKMP()
    val listState = rememberLazyListState()
    var showImageSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(imagePicker.result) {
        when (val result = imagePicker.result) {
            is ImagePickerResult.Success -> {
                onIntent(CreateIntent.ImagesSelected(result.photos.toSelectedImages()))
                imagePicker.reset()
            }
            is ImagePickerResult.Dismissed,
            is ImagePickerResult.Error -> imagePicker.reset()
            ImagePickerResult.Idle,
            ImagePickerResult.Loading -> Unit
        }
    }
    val launchGallery = {
        if (state.images.size >= MaxListingImages) {
            onIntent(CreateIntent.ImageLimitReached)
        } else {
            imagePicker.launchGallery(
                allowMultiple = true,
                selectionLimit = MaxListingImages - state.images.size,
                mimeTypes = listOf(
                    MimeType.IMAGE_JPEG,
                    MimeType.IMAGE_PNG,
                    MimeType.IMAGE_WEBP,
                    MimeType.IMAGE_HEIC,
                ),
            )
        }
    }
    val launchCamera = {
        if (state.images.size >= MaxListingImages) {
            onIntent(CreateIntent.ImageLimitReached)
        } else {
            imagePicker.launchCamera()
        }
    }
    val showImageSourcePicker = {
        if (state.images.size >= MaxListingImages) {
            onIntent(CreateIntent.ImageLimitReached)
        } else {
            showImageSourceSheet = true
        }
    }

    if (showImageSourceSheet) {
        ImageSourceSheet(
            onDismiss = { showImageSourceSheet = false },
            onTakePhoto = {
                showImageSourceSheet = false
                launchCamera()
            },
            onChooseFromGallery = {
                showImageSourceSheet = false
                launchGallery()
            },
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                KupioTopNavbar(
                    title = stringResource(Res.string.topbar_new_listing_title),
                    subtitle = "Create and publish your item",
                    leadingContent = {
                        KupioTopBarBackAction(
                            contentDescription = stringResource(Res.string.topbar_back),
                            onClick = { onIntent(CreateIntent.Back) },
                        )
                    },
                )
            }
        },
        bottomBar = {
            PublishBar(
                state = state,
                onSaveDraft = { onIntent(CreateIntent.Back) },
                onPublish = { onIntent(CreateIntent.Publish) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 16.dp,
                    end = 18.dp,
                    bottom = KupioThemeDefaults.spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = CreateSection.Photos.key) {
                    PhotosSection(
                        images = state.images,
                        imageWarning = state.imageWarning,
                        onAdd = showImageSourcePicker,
                        onRemove = { onIntent(CreateIntent.RemoveImage(it)) },
                    )
                }
                item(key = CreateSection.Details.key) {
                    DetailsSection(
                        state = state,
                        onIntent = onIntent,
                    )
                }
                item(key = CreateSection.Filters.key) {
                    FiltersSection(
                        state = state,
                        onIntent = onIntent,
                    )
                }
                item(key = CreateSection.Price.key) {
                    PriceSection(
                        state = state,
                        onIntent = onIntent,
                    )
                }
                state.submitError?.let { error ->
                    item(key = "submit_error") {
                        ErrorText(text = error)
                    }
                }
            }
        }
    }
}

private enum class CreateSection(
    val key: String,
) {
    Photos("photos"),
    Details("details"),
    Filters("filters"),
    Price("price");

}

@Composable
private fun PhotosSection(
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
                .clickable(onClick = onAdd),
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
private fun ImageSourceSheet(
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
            modifier = Modifier.clickable(onClick = onTakePhoto),
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
            modifier = Modifier.clickable(onClick = onChooseFromGallery),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PhotoPlaceholder(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
            ),
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
            .clickable(onClick = onClick),
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
                .clickable(onClick = onRemove),
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
            .clickable(onClick = onClick),
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

@Composable
private fun DetailsSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Details") {
        CreateTextField(
            label = "Title",
            value = state.title,
            onValueChange = { onIntent(CreateIntent.TitleChanged(it)) },
            placeholder = "What are you selling?",
            error = state.fieldErrors[CreateField.TITLE],
            characterCount = "${state.title.length}/255",
            singleLine = true,
        )
        CreateTextField(
            label = "Description",
            value = state.description,
            onValueChange = { onIntent(CreateIntent.DescriptionChanged(it)) },
            placeholder = "Condition, dimensions, reason for selling...",
            error = state.fieldErrors[CreateField.DESCRIPTION],
            characterCount = "${state.description.length}/5000",
            minLines = 4,
        )
        CategorySelector(
            state = state,
            onIntent = onIntent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FieldLabel(
        label = "Category",
        required = true,
    )
    when {
        state.isLoadingCategories -> LoadingRow()
        state.categoriesError != null -> RetryRow(
            message = "Could not load categories.",
            onRetry = { onIntent(CreateIntent.RetryCategories) },
        )

        else -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.categories, key = { it.id }) { category ->
                    ChoiceChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onIntent(CreateIntent.CategorySelected(category.id)) },
                        text = category.name,
                    )
                }
            }
        }
    }
    state.fieldErrors[CreateField.CATEGORY]?.let { ErrorText(it) }
}

@Composable
private fun FiltersSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Category filters") {
        when {
            state.selectedCategoryId == null -> Text(
                text = "Select a category to see available filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.isLoadingFilters -> LoadingRow()
            state.filtersError != null -> RetryRow(
                message = "Could not load category filters.",
                onRetry = { onIntent(CreateIntent.RetryFilters) },
            )

            state.filters.isEmpty() -> Text(
                text = "No extra details needed for this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> state.filters.forEach { filter ->
                FilterInput(
                    filter = filter,
                    value = state.filterValues[filter.slug],
                    error = state.filterErrors[filter.slug],
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun FilterInput(
    filter: FilterDefinition,
    value: CreateFilterInput?,
    error: String?,
    onIntent: (CreateIntent) -> Unit,
) {
    when (filter.type) {
        FilterType.TEXT -> CreateTextField(
            label = filter.label,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            onValueChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it)) },
            placeholder = filter.label,
            error = error,
            required = filter.isRequired,
            singleLine = true,
        )

        FilterType.NUMBER, FilterType.RANGE -> CreateTextField(
            label = filter.label,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            onValueChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it.numericText())) },
            placeholder = filter.numberPlaceholder(),
            error = error,
            required = filter.isRequired,
            singleLine = true,
            keyboardType = KeyboardType.Decimal,
        )

        FilterType.BOOLEAN -> BooleanFilter(
            filter = filter,
            value = (value as? CreateFilterInput.BooleanValue)?.value,
            error = error,
            onChange = { onIntent(CreateIntent.FilterBooleanChanged(filter.slug, it)) },
        )

        FilterType.SELECT -> SelectFilter(
            filter = filter,
            value = (value as? CreateFilterInput.Text)?.value.orEmpty(),
            error = error,
            onChange = { onIntent(CreateIntent.FilterTextChanged(filter.slug, it)) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SelectFilter(
    filter: FilterDefinition,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    FieldLabel(
        label = filter.label,
        required = filter.isRequired,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filter.options.values.forEach { option ->
            ChoiceChip(
                selected = value == option,
                onClick = { onChange(option) },
                text = option,
            )
        }
    }
    error?.let { ErrorText(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooleanFilter(
    filter: FilterDefinition,
    value: Boolean?,
    error: String?,
    onChange: (Boolean?) -> Unit,
) {
    FieldLabel(
        label = filter.label,
        required = filter.isRequired,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!filter.isRequired) {
            ChoiceChip(
                selected = value == null,
                onClick = { onChange(null) },
                text = "Unset",
            )
        }
        ChoiceChip(
            selected = value == true,
            onClick = { onChange(true) },
            text = "Yes",
        )
        ChoiceChip(
            selected = value == false,
            onClick = { onChange(false) },
            text = "No",
        )
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(99.dp),
        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PriceSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Price") {
        CreatePriceField(
            price = state.price,
            currency = state.currency,
            error = state.fieldErrors[CreateField.PRICE],
            enabled = !state.isFree,
            onPriceChange = { onIntent(CreateIntent.PriceChanged(it.digitsOnly())) },
            onCurrencySelect = { onIntent(CreateIntent.CurrencyChanged(it)) },
        )

        ToggleRow(
            checked = state.isFree,
            title = "Give away for free",
            subtitle = "Price will be sent as 0",
            onClick = { onIntent(CreateIntent.ToggleFree) },
        )
        ToggleRow(
            checked = state.isTradable,
            title = "Open to trades",
            subtitle = "Buyers can offer swaps",
            onClick = { onIntent(CreateIntent.ToggleTradable) },
        )
    }
}

@Composable
private fun CreatePriceField(
    price: String,
    currency: Currency,
    error: String?,
    enabled: Boolean,
    onPriceChange: (String) -> Unit,
    onCurrencySelect: (Currency) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (error == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.16f) else MaterialTheme.colorScheme.error
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Euro,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text("0") },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.6).sp,
                ),
                colors = priceTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
            CurrencyMenu(
                selected = currency,
                onSelect = onCurrencySelect,
            )
        }
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun priceTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    disabledPlaceholderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun CurrencyMenu(
    selected: Currency,
    onSelect: (Currency) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(selected.name)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.name) },
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    checked: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.35f
                            )
                        ),
                        RoundedCornerShape(6.dp),
                    )
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun PublishBar(
    state: CreateState,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onSaveDraft,
                enabled = !state.isSubmitting,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.outline,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Text("Cancel")
            }
            KupioPrimaryButton(
                text = "Publish listing",
                onClick = onPublish,
                modifier = Modifier.weight(1f),
                enabled = state.canSubmit,
                loading = state.isSubmitting,
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.3).sp),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            trailing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        content()
    }
}

@Composable
private fun CreateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    characterCount: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    FieldLabel(
        label = label,
        required = required,
        trailing = characterCount,
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        supportingText = { error?.let { Text(it) } },
        colors = createTextFieldColors(),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun createTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    disabledPlaceholderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun FieldLabel(
    label: String,
    required: Boolean = false,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase() + if (required) " *" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = KupioThemeDefaults.spacing.md),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RetryRow(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        ErrorText(message)
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private fun List<PhotoResult>.toSelectedImages(): List<SelectedListingImage> =
    mapNotNull { photo ->
        val bytes = photo.loadBytes()
        if (bytes.isEmpty()) return@mapNotNull null
        SelectedListingImage(
            id = photo.uri.ifBlank { Random.nextLong().toString() },
            fileName = photo.fileName ?: "listing-photo-${Random.nextLong().toString().takeLast(6)}.jpg",
            mimeType = photo.mimeType ?: "image/jpeg",
            bytes = bytes,
            previewBitmap = photo.loadImageBitmap(),
        )
    }

private fun String.digitsOnly(): String = filter { it.isDigit() }

private fun String.numericText(): String = filterIndexed { index, char ->
    char.isDigit() || char == '.' || (char == '-' && index == 0)
}

private fun FilterDefinition.numberPlaceholder(): String {
    val min = options.min?.formatForDisplay()
    val max = options.max?.formatForDisplay()
    return when {
        min != null && max != null -> "$min - $max"
        min != null -> "At least $min"
        max != null -> "Up to $max"
        else -> label
    }
}

private fun Double.formatForDisplay(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
