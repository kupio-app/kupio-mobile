package kupio.mobile.features.listings.presentation.edit

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.core.network.ApiException
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImage
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateField
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.SelectedListingImage
import kupio.mobile.features.listings.presentation.form.RemoteListingImage

@OptIn(ExperimentalCoroutinesApi::class)
class EditListingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load pre-fills listing form and remote images`() = runTest(dispatcher) {
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(),
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingListing)
        assertEquals("Vintage oak desk", state.form.title)
        assertEquals("Furniture", state.form.selectedCategoryName)
        assertFalse(state.form.isFree)
        assertTrue(state.form.isTradable)
        assertEquals(2, state.images.size)
        assertTrue(state.images.all { it is RemoteListingImage })
    }

    @Test
    fun `save uploads only new images deletes removed images and applies final order`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(
            EditListingIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "new.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        viewModel.onIntent(EditListingIntent.MoveImage(fromIndex = 2, toIndex = 0))
        viewModel.onIntent(EditListingIntent.RemoveImage("remote-image-a"))
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals("listing-1", listings.updatedListingId)
        assertEquals("+421900111222", listings.updatedPhone)
        assertEquals("Elena K.", listings.updatedContactName)
        assertEquals(true, listings.updatedIsCallsDisabled)
        assertEquals(listOf("new.jpg"), listings.uploadedImages.map { it.fileName })
        assertEquals(listOf("image-a"), listings.deletedImageIds)
        assertEquals(listOf("uploaded-0", "image-b"), listings.orderedImageIds)
        assertEquals(EditListingEffect.OpenListing("listing-1"), viewModel.effects.first())
    }

    @Test
    fun `empty title does not save`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(EditListingIntent.FormIntent(CreateIntent.TitleChanged("")))
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(null, listings.updatedListingId)
        assertIs<CreateError.TitleTooShort>(
            viewModel.state.value.form.fieldErrors[CreateField.TITLE],
        )
    }

    @Test
    fun `order failure refreshes current images and retries with all image ids`() = runTest(dispatcher) {
        val listings = FakeListingsRepository(
            firstOrderFailure = ApiException(
                statusCode = 422,
                message = "image_ids must contain all listing images exactly once",
            ),
            refreshedListing = listing("listing-1").copy(
                images = listOf(
                    ListingImage(
                        id = "image-a",
                        url = "https://example.test/a.jpg",
                        sortOrder = 0,
                    ),
                    ListingImage(
                        id = "image-b",
                        url = "https://example.test/b.jpg",
                        sortOrder = 1,
                    ),
                    ListingImage(
                        id = "image-c",
                        url = "https://example.test/c.jpg",
                        sortOrder = 2,
                    ),
                ),
                imageUrls = listOf(
                    "https://example.test/a.jpg",
                    "https://example.test/b.jpg",
                    "https://example.test/c.jpg",
                ),
            ),
        )
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(EditListingIntent.MoveImage(fromIndex = 1, toIndex = 0))
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(
            listOf(
                listOf("image-b", "image-a"),
                listOf("image-b", "image-a", "image-c"),
            ),
            listings.orderAttempts,
        )
        assertEquals(EditListingEffect.OpenListing("listing-1"), viewModel.effects.first())
    }

    @Test
    fun `retry after image upload failure does not upload same local image again`() = runTest(dispatcher) {
        val listings = FakeListingsRepository(
            firstOrderFailure = ApiException(statusCode = 500, message = "Ordering failed."),
        )
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(
            EditListingIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "new.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(1, listings.uploadCalls)
        assertIs<CreateError.ServerMessage>(viewModel.state.value.submitError)

        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(1, listings.uploadCalls)
        assertEquals(
            listOf(
                listOf("image-a", "image-b", "uploaded-0"),
                listOf("image-a", "image-b", "uploaded-0"),
            ),
            listings.orderAttempts,
        )
        assertEquals(EditListingEffect.OpenListing("listing-1"), viewModel.effects.first())
    }

    @Test
    fun `saved uploaded image can be removed by a later save`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(
            EditListingIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "new.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        viewModel.onIntent(EditListingIntent.RemoveImage("remote-uploaded-0"))
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(listOf("uploaded-0"), listings.deletedImageIds)
    }

    @Test
    fun `already deleted image does not block save retry`() = runTest(dispatcher) {
        val listings = FakeListingsRepository(
            deleteFailures = mutableMapOf(
                "image-a" to ApiException(statusCode = 404, message = "Listing image not found"),
            ),
        )
        val viewModel = EditListingViewModel(
            listingId = "listing-1",
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(EditListingIntent.RemoveImage("remote-image-a"))
        viewModel.onIntent(EditListingIntent.Save)
        advanceUntilIdle()

        assertEquals(listOf("image-a"), listings.deletedImageIds)
        assertEquals(listOf("image-b"), listings.orderedImageIds)
        assertEquals(EditListingEffect.OpenListing("listing-1"), viewModel.effects.first())
    }

    private class FakeCategoriesRepository : CategoriesRepository {
        override suspend fun getRootCategories(limit: Int): List<Category> =
            listOf(Category(1, "Furniture", null, 0, null)).take(limit)

        override suspend fun getSubcategories(
            categoryId: Int,
            limit: Int,
            forceRefresh: Boolean,
        ): List<Category> = emptyList()

        override suspend fun getCategoryFilters(
            categoryId: Int,
            forceRefresh: Boolean,
        ): List<FilterDefinition> = emptyList()
    }

    private class FakeListingsRepository(
        private var firstOrderFailure: ApiException? = null,
        private val refreshedListing: Listing? = null,
        private val deleteFailures: MutableMap<String, ApiException> = mutableMapOf(),
    ) : ListingsRepository {
        var updatedListingId: String? = null
        var updatedListing: CreateListing? = null
        var updatedPhone: String? = null
        var updatedContactName: String? = null
        var updatedIsCallsDisabled: Boolean? = null
        var uploadedImages: List<ListingImageUpload> = emptyList()
        var uploadCalls = 0
        val deletedImageIds = mutableListOf<String>()
        var orderedImageIds: List<String> = emptyList()
        val orderAttempts = mutableListOf<List<String>>()
        private var getListingCalls = 0

        override suspend fun getFeed(
            limit: Int,
            cursor: String?,
            query: String?,
            categoryId: Int?,
        ): ListingFeed = ListingFeed(emptyList(), null)

        override suspend fun getListing(id: String): Listing {
            getListingCalls += 1
            return if (getListingCalls > 1 && refreshedListing != null) {
                refreshedListing
            } else {
                listing(id)
            }
        }

        override suspend fun getListingDetail(id: String): Listing = listing(id)

        override suspend fun createListing(listing: CreateListing): Listing = listing("created")

        override suspend fun updateListing(
            listingId: String,
            listing: CreateListing,
            phone: String?,
            contactName: String?,
            isCallsDisabled: Boolean,
        ): Listing {
            updatedListingId = listingId
            updatedListing = listing
            updatedPhone = phone
            updatedContactName = contactName
            updatedIsCallsDisabled = isCallsDisabled
            return listing(listingId)
        }

        override suspend fun updateListingStatus(
            listingId: String,
            status: ListingStatus,
        ): Listing = listing(listingId)

        override suspend fun uploadListingImages(
            listingId: String,
            images: List<ListingImageUpload>,
        ): List<ListingImage> {
            uploadCalls += 1
            uploadedImages = images
            return images.mapIndexed { index, _ ->
                ListingImage(
                    id = "uploaded-$index",
                    url = "https://example.test/uploaded-$index.jpg",
                    sortOrder = index,
                )
            }
        }

        override suspend fun deleteListingImage(listingId: String, imageId: String) {
            deletedImageIds += imageId
            deleteFailures.remove(imageId)?.let { throw it }
        }

        override suspend fun updateListingImagesOrder(listingId: String, imageIds: List<String>) {
            orderAttempts += imageIds
            firstOrderFailure?.let { failure ->
                firstOrderFailure = null
                throw failure
            }
            orderedImageIds = imageIds
        }
    }

    private companion object {
        fun listing(id: String): Listing = Listing(
            id = id,
            title = "Vintage oak desk",
            description = "Solid oak writing desk in good condition with small signs of normal use.",
            price = 180,
            currency = Currency.EUR,
            status = ListingStatus.ACTIVE,
            primaryImageUrl = "https://example.test/a.jpg",
            images = listOf(
                ListingImage(
                    id = "image-a",
                    url = "https://example.test/a.jpg",
                    sortOrder = 0,
                ),
                ListingImage(
                    id = "image-b",
                    url = "https://example.test/b.jpg",
                    sortOrder = 1,
                ),
            ),
            imageUrls = listOf("https://example.test/a.jpg", "https://example.test/b.jpg"),
            createdAt = "2026-04-26T00:00:00Z",
            userId = "seller-1",
            categoryId = 1,
            categoryName = "Furniture",
            seenCount = 12,
            phone = "+421900111222",
            contactName = "Elena K.",
            isCallsDisabled = true,
            isFree = false,
            isTradable = true,
            customFilters = emptyMap(),
        )
    }
}
