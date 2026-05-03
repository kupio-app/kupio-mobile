package kupio.mobile.features.search.domain.model

data class SearchFilters(
    val query: String = "",
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val categoryPath: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val dealType: DealType = DealType.ANY,
    val onlyWithPhotos: Boolean = false,
    val customFilters: Map<String, String> = emptyMap(),
    val sortBy: SearchSortBy = SearchSortBy.RECOMMENDED,
)

enum class DealType {
    ANY, FOR_SALE, FREE, TRADE
}

enum class SearchSortBy {
    RECOMMENDED, NEWEST_FIRST, PRICE_LOW_HIGH, PRICE_HIGH_LOW
}

fun SearchFilters.toApiParams(): Triple<Boolean?, Boolean?, Map<String, String>> {
    val isFree = when (dealType) {
        DealType.FREE -> true
        DealType.FOR_SALE -> false
        else -> null
    }
    val isTradable = when (dealType) {
        DealType.TRADE -> true
        else -> null
    }
    return Triple(isFree, isTradable, customFilters)
}
