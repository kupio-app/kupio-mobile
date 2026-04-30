package kupio.mobile.features.listings.presentation.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingScreen
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.data.image.MaxListingImages
import kupio.mobile.features.listings.presentation.create.components.DetailsSection
import kupio.mobile.features.listings.presentation.create.components.ErrorText
import kupio.mobile.features.listings.presentation.create.components.FiltersSection
import kupio.mobile.features.listings.presentation.create.components.PriceSection
import kupio.mobile.features.listings.presentation.create.toErrorMessage
import kupio.mobile.features.listings.presentation.create.toSelectedImages
import kupio.mobile.features.listings.presentation.detail.ListingDetailScreen
import kupio.mobile.features.listings.presentation.form.ListingFormPhotosSection
import kupio.mobile.features.listings.presentation.form.ListingImageSourceSheet
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.edit_listing_load_error
import mobile.composeapp.generated.resources.edit_listing_save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class EditListingScreen(val listingId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<EditListingViewModel>(
            key = "edit-listing-$listingId",
        ) { parametersOf(listingId) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                EditListingEffect.NavigateBack -> navigator.pop()
                is EditListingEffect.OpenListing -> navigator.replace(ListingDetailScreen(effect.id))
            }
        }

        EditListingContent(
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@Composable
private fun EditListingContent(
    state: EditListingState,
    onIntent: (EditListingIntent) -> Unit,
) {
    val imagePicker = rememberImagePickerKMP()
    val listState = rememberLazyListState()
    var showImageSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(imagePicker.result) {
        when (val result = imagePicker.result) {
            is ImagePickerResult.Success -> {
                val selectedImages = withContext(Dispatchers.Default) {
                    result.photos.toSelectedImages()
                }
                onIntent(EditListingIntent.ImagesSelected(selectedImages))
                imagePicker.reset()
            }
            is ImagePickerResult.Dismissed,
            is ImagePickerResult.Error,
            -> imagePicker.reset()
            ImagePickerResult.Idle,
            ImagePickerResult.Loading,
            -> Unit
        }
    }

    val launchGallery = {
        if (state.images.size >= MaxListingImages) {
            onIntent(EditListingIntent.ImageLimitReached)
        } else {
            imagePicker.launchGallery(
                allowMultiple = true,
                selectionLimit = MaxListingImages - state.images.size,
                mimeTypes = listOf(
                    MimeType.IMAGE_JPEG,
                    MimeType.IMAGE_PNG,
                    MimeType.IMAGE_WEBP,
                ),
            )
        }
    }
    val launchCamera = {
        if (state.images.size >= MaxListingImages) {
            onIntent(EditListingIntent.ImageLimitReached)
        } else {
            imagePicker.launchCamera()
        }
    }
    val showImageSourcePicker = {
        if (state.images.size >= MaxListingImages) {
            onIntent(EditListingIntent.ImageLimitReached)
        } else {
            showImageSourceSheet = true
        }
    }

    if (showImageSourceSheet) {
        ListingImageSourceSheet(
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!state.isLoadingListing && state.loadError == null) {
                SaveBar(
                    enabled = state.canSave,
                    isSaving = state.isSaving,
                    onSave = { onIntent(EditListingIntent.Save) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            state.isLoadingListing -> KupioLoadingScreen()
            state.loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                KupioErrorRetryRow(
                    message = stringResource(Res.string.edit_listing_load_error),
                    onRetry = { onIntent(EditListingIntent.Retry) },
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = KupioThemeDefaults.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "photos") {
                    ListingFormPhotosSection(
                        images = state.images,
                        warningText = state.imageWarning?.toErrorMessage(),
                        onBack = { onIntent(EditListingIntent.Back) },
                        onAdd = showImageSourcePicker,
                        onRemove = { onIntent(EditListingIntent.RemoveImage(it)) },
                        onMove = { from, to -> onIntent(EditListingIntent.MoveImage(from, to)) },
                    )
                }
                item(key = "details") {
                    FormContentPadding {
                        DetailsSection(
                            state = state.form,
                            onIntent = { onIntent(EditListingIntent.FormIntent(it)) },
                        )
                    }
                }
                item(key = "filters") {
                    FormContentPadding {
                        FiltersSection(
                            state = state.form,
                            onIntent = { onIntent(EditListingIntent.FormIntent(it)) },
                        )
                    }
                }
                item(key = "price") {
                    FormContentPadding {
                        PriceSection(
                            state = state.form,
                            onIntent = { onIntent(EditListingIntent.FormIntent(it)) },
                        )
                    }
                }
                state.submitError?.let { error ->
                    item(key = "submit_error") {
                        FormContentPadding {
                            ErrorText(text = error.toErrorMessage())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveBar(
    enabled: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Button(
            onClick = onSave,
            enabled = enabled && !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 24.dp)
                .height(48.dp),
            shape = KupioShapes.Medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(Res.string.edit_listing_save))
            }
        }
    }
}

@Composable
private fun FormContentPadding(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp),
    ) {
        content()
    }
}
