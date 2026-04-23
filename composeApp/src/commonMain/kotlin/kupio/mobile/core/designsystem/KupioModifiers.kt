package kupio.mobile.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

inline fun Modifier.bouncingClickable(
    crossinline onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    val opacity by animateFloatAsState(
        targetValue = if (pressed) 0.7f else 1f,
        label = "opacity"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = opacity
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

inline fun Modifier.glowClickable(
    shape: Shape = RoundedCornerShape(12.dp), // Match your button's corner radius
    crossinline onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }

    // Animate the alpha of the highlight
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.15f else 0f,
        label = "GlowAlpha"
    )

    this
        .clip(shape) // Ensures the glow doesn't leak outside the border
        .drawBehind {
            // Draws the highlight color behind the text but inside the border
            if (highlightAlpha > 0f) {
                drawRect(color = Color.White.copy(alpha = highlightAlpha))
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

fun Modifier.borderTop(
    width: Dp,
    color: Color,
): Modifier = composed {
    this.drawBehind {
        val strokeWidth = width.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )
    }
}

fun Modifier.borderBottom(
    width: Dp,
    color: Color,
): Modifier = composed {
    this.drawBehind {
        val strokeWidth = width.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth
        )
    }
}