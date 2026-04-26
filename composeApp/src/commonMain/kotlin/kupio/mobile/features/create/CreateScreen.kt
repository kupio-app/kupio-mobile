package kupio.mobile.features.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import kotlin.random.Random
import kupio.mobile.core.designsystem.KupioPrimaryButton
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.create.domain.model.SelectedListingImage
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
    var showImagePicker by remember { mutableStateOf(false) }

    if (showImagePicker) {
        GalleryPickerLauncher(
            allowMultiple = true,
            selectionLimit = MaxListingImages.toLong(),
            mimeTypes = listOf(MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG, MimeType.IMAGE_WEBP, MimeType.IMAGE_HEIC),
            onPhotosSelected = { photos ->
                showImagePicker = false
                onIntent(CreateIntent.ImagesSelected(photos.toSelectedImages()))
            },
            onError = { showImagePicker = false },
            onDismiss = { showImagePicker = false },
        )
    }

    Scaffold(
        topBar = {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = KupioThemeDefaults.spacing.lg,
                top = KupioThemeDefaults.spacing.md,
                end = KupioThemeDefaults.spacing.lg,
                bottom = KupioThemeDefaults.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.lg),
        ) {
            item(key = "progress") { CreateProgress() }
            item(key = "photos") {
                PhotosSection(
                    images = state.images,
                    onAdd = { showImagePicker = true },
                    onRemove = { onIntent(CreateIntent.RemoveImage(it)) },
                )
            }
            item(key = "details") {
                DetailsSection(
                    state = state,
                    onIntent = onIntent,
                )
            }
            item(key = "filters") {
                FiltersSection(
                    state = state,
                    onIntent = onIntent,
                )
            }
            item(key = "price") {
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

@Composable
private fun CreateProgress() {
    val steps = listOf("Photos", "Details", "Filters", "Price")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (index == 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            },
                        ),
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PhotosSection(
    images: List<SelectedListingImage>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    FormSection(
        title = "Photos",
        trailing = "Up to $MaxListingImages",
    ) {
        val cover = images.firstOrNull()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
            ) {
                Icon(
                    imageVector = if (cover == null) Icons.Outlined.PhotoCamera else Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = cover?.fileName ?: "Tap to add photos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (cover == null) "JPG, PNG or WEBP" else "${images.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (cover != null) {
                CoverBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
            contentPadding = PaddingValues(top = KupioThemeDefaults.spacing.sm),
        ) {
            items(images, key = { it.id }) { image ->
                ImageTile(
                    image = image,
                    onRemove = { onRemove(image.id) },
                )
            }
            if (images.size < MaxListingImages) {
                item(key = "add") {
                    AddImageTile(onClick = onAdd)
                }
            }
        }
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
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ImageTile(
    image: SelectedListingImage,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${image.fileName}",
                modifier = Modifier.size(16.dp),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onIntent(CreateIntent.CategorySelected(category.id)) },
                        label = { Text(category.name) },
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
    if (state.selectedCategoryId == null) return

    FormSection(title = "Category filters") {
        when {
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
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        filter.options.values.forEach { option ->
            FilterChip(
                selected = value == option,
                onClick = { onChange(option) },
                label = { Text(option) },
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
    Row(horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
        if (!filter.isRequired) {
            FilterChip(
                selected = value == null,
                onClick = { onChange(null) },
                label = { Text("Unset") },
            )
        }
        FilterChip(
            selected = value == true,
            onClick = { onChange(true) },
            label = { Text("Yes") },
        )
        FilterChip(
            selected = value == false,
            onClick = { onChange(false) },
            label = { Text("No") },
        )
    }
    error?.let { ErrorText(it) }
}

@Composable
private fun PriceSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = "Price") {
        Row(horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
            OutlinedTextField(
                value = state.price,
                onValueChange = { onIntent(CreateIntent.PriceChanged(it.digitsOnly())) },
                modifier = Modifier.weight(1f),
                label = { Text("Price") },
                leadingIcon = { Icon(Icons.Outlined.Euro, contentDescription = null) },
                isError = state.fieldErrors[CreateField.PRICE] != null,
                enabled = !state.isFree,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = {
                    state.fieldErrors[CreateField.PRICE]?.let { Text(it) }
                },
                shape = RoundedCornerShape(12.dp),
            )
            CurrencyMenu(
                selected = state.currency,
                onSelect = { onIntent(CreateIntent.CurrencyChanged(it)) },
            )
        }

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
private fun CurrencyMenu(
    selected: Currency,
    onSelect: (Currency) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = KupioThemeDefaults.defaultBorder,
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
        border = KupioThemeDefaults.defaultBorder,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KupioThemeDefaults.spacing.lg, vertical = KupioThemeDefaults.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
        ) {
            TextButton(
                onClick = onSaveDraft,
                enabled = !state.isSubmitting,
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
    Column(verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            trailing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = RoundedCornerShape(12.dp),
    )
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun List<GalleryPhotoResult>.toSelectedImages(): List<SelectedListingImage> =
    mapNotNull { photo ->
        val bytes = photo.loadBytes()
        if (bytes.isEmpty()) return@mapNotNull null
        SelectedListingImage(
            id = photo.uri.ifBlank { Random.nextLong().toString() },
            fileName = photo.fileName ?: "listing-photo-${Random.nextLong().toString().takeLast(6)}.jpg",
            mimeType = photo.mimeType ?: "image/jpeg",
            bytes = bytes,
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
