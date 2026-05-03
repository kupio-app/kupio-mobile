package kupio.mobile.features.search.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentSearch(
    val query: String,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val timestamp: Long = 0L,
)
