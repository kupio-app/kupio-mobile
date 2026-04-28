package kupio.mobile.features.createlisting.presentation.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.createlisting.MaxListingImages
import kupio.mobile.features.createlisting.presentation.create.components.DetailsSection
import kupio.mobile.features.createlisting.presentation.create.components.ErrorText
import kupio.mobile.features.createlisting.presentation.create.components.FiltersSection
import kupio.mobile.features.createlisting.presentation.create.components.ImageSourceSheet
import kupio.mobile.features.createlisting.presentation.create.components.PhotosSection
import kupio.mobile.features.createlisting.presentation.create.components.PriceSection
import kupio.mobile.features.createlisting.presentation.create.components.PublishBar
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_topbar_subtitle
import mobile.composeapp.generated.resources.topbar_back
import mobile.composeapp.generated.resources.topbar_new_listing_title
import org.jetbrains.compose.resources.stringResource
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
                    subtitle = stringResource(Res.string.create_topbar_subtitle),
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
