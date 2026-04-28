package kupio.mobile.features.listings.domain.model

data class FilterDefinition(
    val id: Int,
    val categoryId: Int,
    val slug: String,
    val label: String,
    val type: FilterType,
    val options: FilterOptions,
    val isRequired: Boolean,
    val displayOrder: Int,
)

enum class FilterType {
    TEXT,
    NUMBER,
    BOOLEAN,
    SELECT,
    RANGE,
}

data class FilterOptions(
    val values: List<String> = emptyList(),
    val min: Double? = null,
    val max: Double? = null,
)
