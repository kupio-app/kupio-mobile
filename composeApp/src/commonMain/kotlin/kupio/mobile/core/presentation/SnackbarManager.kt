package kupio.mobile.core.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SnackbarEvent(
    val icon: ImageVector,
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

class SnackbarManager {
    private val _events = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SnackbarEvent> = _events.asSharedFlow()

    fun show(event: SnackbarEvent) {
        _events.tryEmit(event)
    }
}
