package kupio.mobile.features.create

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterOptions
import kupio.mobile.features.listings.domain.model.FilterType

class CreateValidationTest {

    @Test
    fun `valid state builds custom filter payload`() {
        val result = validateCreateListing(
            validState().copy(
                filters = listOf(
                    filter(slug = "ram", type = FilterType.SELECT, options = FilterOptions(values = listOf("16 GB"))),
                    filter(slug = "screen_size", type = FilterType.NUMBER),
                    filter(slug = "has_box", type = FilterType.BOOLEAN),
                ),
                filterValues = mapOf(
                    "ram" to CreateFilterInput.Text("16 GB"),
                    "screen_size" to CreateFilterInput.Text("15.6"),
                    "has_box" to CreateFilterInput.BooleanValue(true),
                ),
            ),
        )

        assertTrue(result.isValid)
        assertEquals(
            mapOf(
                "ram" to CustomFilterPayloadValue.Text("16 GB"),
                "screen_size" to CustomFilterPayloadValue.Number(15.6),
                "has_box" to CustomFilterPayloadValue.BooleanValue(true),
            ),
            result.listing?.customFilters,
        )
    }

    @Test
    fun `missing required filter blocks submit`() {
        val result = validateCreateListing(
            validState().copy(
                filters = listOf(filter(slug = "ram", type = FilterType.SELECT, isRequired = true)),
                filterValues = emptyMap(),
            ),
        )

        assertFalse(result.isValid)
        assertEquals("Required.", result.filterErrors["ram"])
    }

    @Test
    fun `range filter validates backend min max options`() {
        val result = validateCreateListing(
            validState().copy(
                filters = listOf(
                    filter(
                        slug = "screen_size",
                        type = FilterType.RANGE,
                        options = FilterOptions(min = 10.0, max = 14.0),
                    ),
                ),
                filterValues = mapOf("screen_size" to CreateFilterInput.Text("15")),
            ),
        )

        assertFalse(result.isValid)
        assertEquals("Must be at most 14.", result.filterErrors["screen_size"])
    }

    @Test
    fun `free listing sends zero price`() {
        val result = validateCreateListing(
            validState().copy(
                isFree = true,
                price = "",
            ),
        )

        assertTrue(result.isValid)
        assertEquals(0, result.listing?.price)
    }

    private fun validState(): CreateState = CreateState(
        title = "Vintage oak desk",
        description = "Solid oak writing desk in good condition with small signs of normal use.",
        price = "180",
        selectedCategoryId = 1,
        selectedCategoryName = "Furniture",
        categories = listOf(Category(1, "Furniture", null, 0, null)),
        isLoadingCategories = false,
    )

    private fun filter(
        slug: String,
        type: FilterType,
        options: FilterOptions = FilterOptions(),
        isRequired: Boolean = false,
    ): FilterDefinition = FilterDefinition(
        id = slug.hashCode(),
        categoryId = 1,
        slug = slug,
        label = slug,
        type = type,
        options = options,
        isRequired = isRequired,
        displayOrder = 0,
    )
}
