package kupio.mobile.features.listings.domain.model

data class Category(
    val id: Int,
    val name: String,
    val iconSlug: String?,
    val depth: Int,
    val parentId: Int?,
)
