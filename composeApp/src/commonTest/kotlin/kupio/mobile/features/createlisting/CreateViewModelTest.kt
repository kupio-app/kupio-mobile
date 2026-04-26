package kupio.mobile.features.createlisting

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kupio.mobile.features.createlisting.domain.model.SelectedListingImage
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterOptions
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.listings.domain.model.Listing
import kupio.mobile.features.listings.domain.model.ListingFeed
import kupio.mobile.features.listings.domain.model.ListingImageUpload
import kupio.mobile.features.listings.domain.repository.CategoriesRepository
import kupio.mobile.features.listings.domain.repository.ListingsRepository

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
    fun `publish creates listing uploads images and navigates back`() = runTest(dispatcher) {
        val listings = FakeListingsRepository()
        val viewModel = CreateViewModel(
            listingsRepository = listings,
            categoriesRepository = FakeCategoriesRepository(
                filtersByCategory = mapOf(
                    1 to listOf(filter(slug = "ram", type = FilterType.SELECT, options = FilterOptions(values = listOf("16 GB")))),
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
        assertEquals(CreateEffect.NavigateBack, viewModel.effects.first())
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
        assertEquals("You can add up to 8 photos.", state.imageWarning)
    }

    private class FakeCategoriesRepository(
        private val categories: List<Category> = listOf(
            Category(1, "Furniture", null, 0, null),
            Category(2, "Electronics", null, 0, null),
        ),
        private val filtersByCategory: Map<Int, List<FilterDefinition>> = emptyMap(),
    ) : CategoriesRepository {
        override suspend fun getRootCategories(limit: Int): List<Category> = categories.take(limit)

        override suspend fun getCategoryFilters(categoryId: Int): List<FilterDefinition> =
            filtersByCategory[categoryId].orEmpty()
    }

    private class FakeListingsRepository : ListingsRepository {
        var createdListing: CreateListing? = null
        var uploadedListingId: String? = null
        var uploadedImages: List<ListingImageUpload> = emptyList()

        override suspend fun getFeed(
            limit: Int,
            cursor: String?,
            query: String?,
            categoryId: Int?,
        ): ListingFeed = ListingFeed(emptyList(), null)

        override suspend fun getListing(id: String): Listing = listing(id)

        override suspend fun createListing(listing: CreateListing): Listing {
            createdListing = listing
            return listing("created-listing")
        }

        override suspend fun uploadListingImages(
            listingId: String,
            images: List<ListingImageUpload>,
        ) {
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
            currency = kupio.mobile.features.listings.domain.model.Currency.EUR,
            primaryImageUrl = null,
            createdAt = "2026-04-26T00:00:00Z",
            categoryId = 1,
            categoryName = "Furniture",
        )
    }
}
