package kupio.mobile.features.listings.presentation.create

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.Currency
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterOptions
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.model.ListingStatus
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository
import kupio.mobile.features.listings.presentation.create.CreateEffect
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateFilterInput
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateViewModel
import kupio.mobile.features.listings.presentation.create.SelectedListingImage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateViewModelTest {
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
    fun `selecting category loads filters and clears stale values`() = runTest(dispatcher) {
        val categories = FakeCategoriesRepository(
            filtersByCategory = mapOf(
                1 to listOf(filter(slug = "ram")),
                2 to listOf(filter(slug = "size")),
            ),
        )
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = categories,
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.CategorySelected(1))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.FilterTextChanged("ram", "16 GB"))
        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.selectedCategoryId)
        assertEquals(listOf("size"), state.filters.map { it.slug })
        assertTrue(state.filterValues.isEmpty())
    }

    @Test
    fun `selecting parent category keeps it selected and loads subcategories`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(
                subcategoriesByCategory = mapOf(
                    2 to listOf(Category(3, "Phones", null, 1, 2)),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.selectedCategoryId)
        assertEquals(listOf("Electronics"), state.categoryPath.map { it.name })
        assertEquals(listOf("Phones"), state.visibleSubcategories.map { it.name })
    }

    @Test
    fun `selecting subcategory extends path and loads its filters`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(
                subcategoriesByCategory = mapOf(
                    2 to listOf(Category(3, "Phones", null, 1, 2)),
                ),
                filtersByCategory = mapOf(
                    3 to listOf(filter(slug = "storage")),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.CategorySelected(3))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.selectedCategoryId)
        assertEquals(listOf("Electronics", "Phones"), state.categoryPath.map { it.name })
        assertEquals(listOf("storage"), state.filters.map { it.slug })
    }

    @Test
    fun `category picker back returns to previous layer`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(
                subcategoriesByCategory = mapOf(
                    2 to listOf(Category(3, "Phones", null, 1, 2)),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.CategorySelected(3))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.CategoryPickerBack)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.selectedCategoryId)
        assertEquals(listOf("Electronics"), state.categoryPath.map { it.name })
    }

    @Test
    fun `selecting same category keeps entered filter values`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(
                filtersByCategory = mapOf(
                    2 to listOf(filter(slug = "condition")),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.FilterTextChanged("condition", "new"))
        viewModel.onIntent(CreateIntent.CategorySelected(2))
        advanceUntilIdle()

        val value = viewModel.state.value.filterValues["condition"] as? CreateFilterInput.Text
        assertEquals("new", value?.value)
    }

    @Test
    fun `publish creates listing uploads images and navigates back`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(
                filtersByCategory = mapOf(
                    1 to listOf(
                        filter(
                            slug = "ram",
                            type = FilterType.SELECT,
                            options = FilterOptions(values = listOf("16 GB"))
                        )
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.FilterTextChanged("ram", "16 GB"))
        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "desk.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()

        assertEquals("Vintage oak desk", listings.createdListing?.title)
        assertEquals(
            mapOf("ram" to CustomFilterPayloadValue.Text("16 GB")),
            listings.createdListing?.customFilters,
        )
        assertEquals("created-listing", listings.uploadedListingId)
        assertEquals(1, listings.uploadedImages.size)
        assertEquals("desk.jpg", listings.uploadedImages.first().fileName)
        assertEquals("created-listing" to ListingStatus.ACTIVE, listings.statusUpdates.single())
        assertEquals(CreateEffect.NavigateBack, viewModel.effects.first())
    }

    @Test
    fun `double publish while submitting creates listing once`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.Publish)
        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()

        assertEquals(1, listings.createCalls)
    }

    @Test
    fun `field edits are ignored while submitting`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.Publish)
        viewModel.onIntent(CreateIntent.TitleChanged("Changed while submitting"))
        viewModel.onIntent(CreateIntent.PriceChanged("999"))
        viewModel.onIntent(CreateIntent.ToggleFree)

        val state = viewModel.state.value
        assertTrue(state.isSubmitting)
        assertEquals("Vintage oak desk", state.title)
        assertEquals("180", state.price)
        assertFalse(state.isFree)
    }

    @Test
    fun `save draft creates listing and uploads images without activating`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        advanceUntilIdle()
        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "desk.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )

        viewModel.onIntent(CreateIntent.SaveDraft)
        advanceUntilIdle()

        assertEquals("Vintage oak desk", listings.createdListing?.title)
        assertEquals("created-listing", listings.uploadedListingId)
        assertTrue(listings.statusUpdates.isEmpty())
        assertEquals(CreateEffect.NavigateBack, viewModel.effects.first())
    }

    @Test
    fun `retry after image upload failure reuses created listing`() = runTest(dispatcher) {
        val listings = FakeListingsRepository().apply {
            failNextUpload = true
        }
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "desk.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()

        assertEquals(1, listings.createCalls)
        assertEquals(2, listings.uploadCalls)
        assertEquals("created-listing" to ListingStatus.ACTIVE, listings.statusUpdates.single())
    }

    @Test
    fun `retry after status failure skips successful image upload`() = runTest(dispatcher) {
        val listings = FakeListingsRepository().apply {
            failNextStatusUpdate = true
        }
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.TitleChanged("Vintage oak desk"))
        viewModel.onIntent(CreateIntent.DescriptionChanged("Solid oak writing desk in good condition with small signs of normal use."))
        viewModel.onIntent(CreateIntent.PriceChanged("180"))
        viewModel.onIntent(CreateIntent.CategorySelected(1))
        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "local-1",
                        fileName = "desk.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()
        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()

        assertEquals(1, listings.createCalls)
        assertEquals(1, listings.uploadCalls)
        assertEquals(2, listings.statusUpdateCalls)
        assertEquals("created-listing" to ListingStatus.ACTIVE, listings.statusUpdates.single())
    }

    @Test
    fun `invalid publish keeps user on screen with validation errors`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(CreateIntent.Publish)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.fieldErrors.isEmpty())
        assertEquals(null, listings.createdListing)
    }

    @Test
    fun `adding more than eight images keeps first eight and shows warning`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                (1..10).map { index ->
                    SelectedListingImage(
                        id = "local-$index",
                        fileName = "photo-$index.jpg",
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf(index.toByte()),
                    )
                },
            ),
        )

        val state = viewModel.state.value
        assertEquals(8, state.images.size)
        assertEquals(CreateError.ImageLimitReached(8), state.imageWarning)
    }

    @Test
    fun `adding unsupported images rejects them with warning`() = runTest(dispatcher) {
        val viewModel = CreateViewModel(
            listingsRepository = FakeListingsRepository(),
            categoriesRepository = FakeCategoriesRepository(),
        )
        advanceUntilIdle()

        viewModel.onIntent(
            CreateIntent.ImagesSelected(
                listOf(
                    SelectedListingImage(
                        id = "heic-photo",
                        fileName = "photo.heic",
                        mimeType = "image/heic",
                        bytes = byteArrayOf(1, 2, 3),
                    ),
                ),
            ),
        )

        val state = viewModel.state.value
        assertTrue(state.images.isEmpty())
        assertEquals(CreateError.UnsupportedImage, state.imageWarning)
    }

    private class FakeCategoriesRepository(
        private val categories: List<Category> = listOf(
            Category(1, "Furniture", null, 0, null),
            Category(2, "Electronics", null, 0, null),
        ),
        private val subcategoriesByCategory: Map<Int, List<Category>> = emptyMap(),
        private val filtersByCategory: Map<Int, List<FilterDefinition>> = emptyMap(),
    ) : CategoriesRepository {
        override suspend fun getRootCategories(limit: Int): List<Category> = categories.take(limit)

        override suspend fun getSubcategories(
            categoryId: Int,
            limit: Int,
            forceRefresh: Boolean,
        ): List<Category> = subcategoriesByCategory[categoryId].orEmpty().take(limit)

        override suspend fun getCategoryFilters(
            categoryId: Int,
            forceRefresh: Boolean,
        ): List<FilterDefinition> =
            filtersByCategory[categoryId].orEmpty()
    }

    private class FakeListingsRepository : ListingsRepository {
        var createCalls = 0
        var uploadCalls = 0
        var statusUpdateCalls = 0
        var failNextUpload = false
        var failNextStatusUpdate = false
        var createdListing: CreateListing? = null
        var uploadedListingId: String? = null
        var uploadedImages: List<ListingImageUpload> = emptyList()
        val statusUpdates = mutableListOf<Pair<String, ListingStatus>>()

        override suspend fun getFeed(
            limit: Int,
            cursor: String?,
            query: String?,
            categoryId: Int?,
        ): ListingFeed = ListingFeed(emptyList(), null)

        override suspend fun getListing(id: String): Listing = listing(id)

        override suspend fun getListingDetail(id: String): Listing = listing(id)

        override suspend fun createListing(listing: CreateListing): Listing {
            createCalls += 1
            createdListing = listing
            return listing("created-listing")
        }

        override suspend fun updateListingStatus(
            listingId: String,
            status: ListingStatus,
        ): Listing {
            statusUpdateCalls += 1
            if (failNextStatusUpdate) {
                failNextStatusUpdate = false
                error("Status update failed")
            }
            statusUpdates += listingId to status
            return listing(listingId)
        }

        override suspend fun uploadListingImages(
            listingId: String,
            images: List<ListingImageUpload>,
        ) {
            uploadCalls += 1
            if (failNextUpload) {
                failNextUpload = false
                error("Upload failed")
            }
            uploadedListingId = listingId
            uploadedImages = images
        }
    }

    private companion object {
        fun filter(
            slug: String,
            type: FilterType = FilterType.TEXT,
            options: FilterOptions = FilterOptions(),
        ): FilterDefinition = FilterDefinition(
            id = slug.hashCode(),
            categoryId = 1,
            slug = slug,
            label = slug,
            type = type,
            options = options,
            isRequired = false,
            displayOrder = 0,
        )

        fun listing(id: String): Listing = Listing(
            id = id,
            title = "Vintage oak desk",
            description = "Solid oak writing desk in good condition with small signs of normal use.",
            price = 180,
            currency = Currency.EUR,
            primaryImageUrl = null,
            imageUrls = emptyList(),
            createdAt = "2026-04-26T00:00:00Z",
            userId = "seller",
            categoryId = 1,
            categoryName = "Furniture",
            seenCount = 0,
            phone = null,
            contactName = null,
            isCallsDisabled = false,
            customFilters = emptyMap(),
        )
    }
}
