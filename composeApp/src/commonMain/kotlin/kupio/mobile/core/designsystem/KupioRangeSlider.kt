package kupio.mobile.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun KupioRangeSlider(
    startValue: Float,
    endValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (start: Float, end: Float) -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 10.dp.toPx() }

    val currentStart by rememberUpdatedState(startValue)
    val currentEnd by rememberUpdatedState(endValue)
    val currentCallback by rememberUpdatedState(onValueChange)

    var sliderWidthPx by remember { mutableStateOf(0f) }
    var draggingThumb by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onGloballyPositioned { coords -> sliderWidthPx = coords.size.width.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tw = (sliderWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val range = valueRange.endInclusive - valueRange.start
                        val sf = if (range == 0f) 0f else ((currentStart - valueRange.start) / range).coerceIn(0f, 1f)
                        val ef = if (range == 0f) 1f else ((currentEnd - valueRange.start) / range).coerceIn(0f, 1f)
                        val sx = sf * tw + thumbRadiusPx
                        val ex = ef * tw + thumbRadiusPx
                        draggingThumb = if (abs(offset.x - sx) <= abs(offset.x - ex)) 0 else 1
                    },
                    onDragEnd = { draggingThumb = null },
                    onDragCancel = { draggingThumb = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val tw = (sliderWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val f = ((change.position.x - thumbRadiusPx) / tw).coerceIn(0f, 1f)
                        val v = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
                        when (draggingThumb) {
                            0 -> currentCallback(v.coerceAtMost(currentEnd), currentEnd)
                            1 -> currentCallback(currentStart, v.coerceAtLeast(currentStart))
                        }
                    },
                )
            },
    ) {
        val tw = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val range = valueRange.endInclusive - valueRange.start
        val sf = if (range == 0f) 0f else ((currentStart - valueRange.start) / range).coerceIn(0f, 1f)
        val ef = if (range == 0f) 1f else ((currentEnd - valueRange.start) / range).coerceIn(0f, 1f)
        val sx = sf * tw + thumbRadiusPx
        val ex = ef * tw + thumbRadiusPx
        val trackH = 4.dp.toPx()
        val cr = CornerRadius(trackH / 2)

        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(thumbRadiusPx, centerY - trackH / 2),
            size = Size(tw, trackH),
            cornerRadius = cr,
        )
        if (ex > sx) {
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(sx, centerY - trackH / 2),
                size = Size(ex - sx, trackH),
                cornerRadius = cr,
            )
        }
        listOf(sx, ex).forEach { cx ->
            drawCircle(color = primaryColor, radius = thumbRadiusPx, center = Offset(cx, centerY))
            drawCircle(color = Color.White, radius = thumbRadiusPx - 2.5f, center = Offset(cx, centerY))
        }
    }
}