package kupio.mobile.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.standaloneTopBarInsetsPadding(): Modifier = composed {
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    )
}
