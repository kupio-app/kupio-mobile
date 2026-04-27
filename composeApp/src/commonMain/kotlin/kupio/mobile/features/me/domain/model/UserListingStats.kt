package kupio.mobile.features.me.domain.model

data class UserListingStats(
    val activeCount: Int,
    val inactiveCount: Int,
    val promotedCount: Int,
    val chatsCount: Int,
    val favouritesCount: Int,
)
