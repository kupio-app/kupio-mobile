package kupio.mobile.features.saved.domain.repository

import kotlinx.coroutines.flow.Flow
import kupio.mobile.features.listings.domain.model.ListingFeed

interface FavouritesRepository {
    val favouriteIds: Flow<Set<String>>
    suspend fun getFavourites(limit: Int = 20, cursor: String? = null): ListingFeed
    suspend fun getFavouriteIds(): Set<String>
    suspend fun addFavourite(listingId: String)
    suspend fun removeFavourite(listingId: String)
}
