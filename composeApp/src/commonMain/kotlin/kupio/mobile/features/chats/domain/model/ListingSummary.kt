package kupio.mobile.features.chats.domain.model

data class ListingSummary(
    val id: String,
    val title: String,
    val priceFormatted: String,
    val placeholderSeed: Int,
    val imageUrl: String? = null,
)
