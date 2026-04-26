package kupio.mobile.features.listings.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kupio.mobile.features.listings.domain.model.Category
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterOptions
import kupio.mobile.features.listings.domain.model.FilterType

@Serializable
data class CategoryResponseDto(
    val id: Int,
    val name: String,
    val depth: Int,
    val icon: String? = null,
    val parent: CategorySlimDto? = null,
)

fun CategoryResponseDto.toDomain(): Category = Category(
    id = id,
    name = name,
    iconSlug = icon,
    depth = depth,
    parentId = parent?.id,
)

@Serializable
data class FilterDefinitionResponseDto(
    val id: Int,
    @SerialName("category_id") val categoryId: Int,
    val slug: String,
    val label: String,
    @SerialName("filter_type") val filterType: FilterTypeDto,
    val options: JsonObject? = null,
    @SerialName("is_required") val isRequired: Boolean,
    @SerialName("display_order") val displayOrder: Int,
)

@Serializable
enum class FilterTypeDto {
    @SerialName("text") TEXT,
    @SerialName("number") NUMBER,
    @SerialName("boolean") BOOLEAN,
    @SerialName("select") SELECT,
    @SerialName("range") RANGE,
}

fun FilterDefinitionResponseDto.toDomain(): FilterDefinition = FilterDefinition(
    id = id,
    categoryId = categoryId,
    slug = slug,
    label = label,
    type = filterType.toDomain(),
    options = options.toFilterOptions(),
    isRequired = isRequired,
    displayOrder = displayOrder,
)

private fun FilterTypeDto.toDomain(): FilterType = when (this) {
    FilterTypeDto.TEXT -> FilterType.TEXT
    FilterTypeDto.NUMBER -> FilterType.NUMBER
    FilterTypeDto.BOOLEAN -> FilterType.BOOLEAN
    FilterTypeDto.SELECT -> FilterType.SELECT
    FilterTypeDto.RANGE -> FilterType.RANGE
}

private fun JsonObject?.toFilterOptions(): FilterOptions {
    if (this == null) return FilterOptions()
    val values = this["values"]
        .let { it as? JsonArray }
        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        .orEmpty()
    return FilterOptions(
        values = values,
        min = this["min"]?.jsonPrimitive?.doubleOrNull,
        max = this["max"]?.jsonPrimitive?.doubleOrNull,
    )
}
