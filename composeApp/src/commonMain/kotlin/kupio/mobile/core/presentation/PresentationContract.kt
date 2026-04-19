package kupio.mobile.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

interface UiState

interface UiAction

interface UiEffect

@Composable
fun <T> CollectEffect(
    effectFlow: Flow<T>,
    onEffect: suspend (T) -> Unit,
) {
    LaunchedEffect(effectFlow) {
        effectFlow.collect(onEffect)
    }
}
