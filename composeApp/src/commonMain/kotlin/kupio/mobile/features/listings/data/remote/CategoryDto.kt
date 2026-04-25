package kupio.mobile.features.listings.data.remote

import kotlinx.serialization.Serializable
import kupio.mobile.features.listings.domain.model.Category

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
