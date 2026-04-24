package kupio.mobile.features.home.domain.model

data class Category(
    val id: Int,
    val name: String,
    val iconSlug: String?,
    val depth: Int,
    val parentId: Int?,
)
