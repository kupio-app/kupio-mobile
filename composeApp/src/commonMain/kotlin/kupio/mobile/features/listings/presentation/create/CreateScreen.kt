package kupio.mobile.features.listings.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.presentation.components.ListingTopBar
import kupio.mobile.features.listings.data.image.MaxListingImages
import kupio.mobile.features.listings.presentation.create.components.DetailsSection
import kupio.mobile.core.designsystem.KupioErrorText
import kupio.mobile.features.listings.presentation.create.components.FiltersSection
import kupio.mobile.features.listings.presentation.create.components.PriceSection
import kupio.mobile.features.listings.presentation.create.components.PublishBar
import kupio.mobile.features.listings.presentation.form.ListingFormPhotosSection
import kupio.mobile.features.listings.presentation.form.ListingImageSourceSheet
import org.koin.compose.viewmodel.koinViewModel

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

@Composable
private fun CreateContent(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    val imagePicker = rememberImagePickerKMP()
    var showImageSourceSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(imagePicker.result) {
        when (val result = imagePicker.result) {
            is ImagePickerResult.Success -> {
                val selectedImages = withContext(Dispatchers.Default) {
                    result.photos.toSelectedImages()
                }
                onIntent(CreateIntent.ImagesSelected(selectedImages))
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

    Box(modifier = Modifier.testTag("listing.create.screen")) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                PublishBar(
                    state = state,
                    onSaveDraft = { onIntent(CreateIntent.SaveDraft) },
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
                    contentPadding = PaddingValues(bottom = KupioThemeDefaults.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = CreateSection.Photos.key) {
                        ListingFormPhotosSection(
                            images = state.images,
                            warningText = state.imageWarning?.toErrorMessage(),
                            onAdd = showImageSourcePicker,
                            onRemove = { onIntent(CreateIntent.RemoveImage(it)) },
                            onMove = { from, to -> onIntent(CreateIntent.MoveImage(from, to)) },
                        )
                    }
                    item(key = CreateSection.Details.key) {
                        FormContentPadding {
                            DetailsSection(
                                state = state,
                                onIntent = onIntent,
                            )
                        }
                    }
                    item(key = CreateSection.Filters.key) {
                        FormContentPadding {
                            FiltersSection(
                                state = state,
                                onIntent = onIntent,
                            )
                        }
                    }
                    item(key = CreateSection.Price.key) {
                        FormContentPadding {
                            PriceSection(
                                state = state,
                                onIntent = onIntent,
                            )
                        }
                    }
                    state.submitError?.let { error ->
                        item(key = "submit_error") {
                            FormContentPadding {
                                KupioErrorText(text = error.toErrorMessage())
                            }
                        }
                    }
                }
            }
        }
        ListingTopBar(
            listState = listState,
            onBack = { onIntent(CreateIntent.Back) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("listing.create.topbar"),
            backButtonModifier = Modifier.testTag("listing.create.back"),
        )
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

private enum class CreateSection(
    val key: String,
) {
    Photos("photos"),
    Details("details"),
    Filters("filters"),
    Price("price");
}
