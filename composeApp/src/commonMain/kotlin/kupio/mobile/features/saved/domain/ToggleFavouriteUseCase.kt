package kupio.mobile.features.saved.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import kotlinx.coroutines.CancellationException
import kupio.mobile.core.presentation.SnackbarEvent
import kupio.mobile.core.presentation.SnackbarManager
import kupio.mobile.features.saved.domain.repository.FavouritesRepository
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.favourite_added
import mobile.composeapp.generated.resources.favourite_removed
import org.jetbrains.compose.resources.getString

class ToggleFavouriteUseCase(
    private val favouritesRepository: FavouritesRepository,
    private val snackbarManager: SnackbarManager,
) {
    suspend operator fun invoke(listingId: String, adding: Boolean): Result<Unit> =
        runCatching {
            if (adding) favouritesRepository.addFavourite(listingId)
            else favouritesRepository.removeFavourite(listingId)
        }.onSuccess {
            val message = getString(if (adding) Res.string.favourite_added else Res.string.favourite_removed)
            val icon = if (adding) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            snackbarManager.show(SnackbarEvent(icon = icon, message = message))
        }.onFailure { t ->
            if (t is CancellationException) throw t
        }
}
