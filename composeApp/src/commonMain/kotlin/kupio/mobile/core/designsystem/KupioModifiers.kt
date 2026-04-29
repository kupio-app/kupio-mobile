package kupio.mobile.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

inline fun Modifier.bouncingClickable(
    crossinline onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    val opacity by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        label = "opacity"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = opacity
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable the default ripple
            onClick = { onClick() }
        )
}

inline fun Modifier.glowClickable(
    shape: Shape = KupioShapes.Medium,
    crossinline onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.15f else 0f,
        label = "GlowAlpha"
    )

    this
        .clip(shape)
        .drawBehind {
            if (highlightAlpha > 0f) {
                drawRect(color = Color.White.copy(alpha = highlightAlpha))
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable the default gray ripple
            onClick = { onClick() }
        )
}

fun Modifier.bouncingDimClickableIf(
    shape: Shape = KupioShapes.Large,
    onClick: (() -> Unit)?,
): Modifier = if (onClick != null) bouncingDimClickable(shape = shape, onClick = onClick) else this

inline fun Modifier.bouncingDimClickable(
    shape: Shape = KupioShapes.Large,
    crossinline onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val overlayColor = MaterialTheme.colorScheme.onSurface

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.06f else 0f,
        label = "pressOverlay"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (overlayAlpha > 0f) drawRect(color = overlayColor.copy(alpha = overlayAlpha))
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick() }
        )
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