package com.example.minimusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Material-style determinate music seekbar.
 *
 * The bar uses one pointer gesture pipeline so taps and drags cannot compete
 * for the same pointer stream. During a drag, the local fraction moves
 * immediately and the media seek is committed only when the gesture ends,
 * preventing controller churn and visible jitter. The drawing has a bright
 * active segment, a distinct inactive segment separated by a small gap, and a
 * 4dp stop indicator at the far end of the track.
 */
@Composable
fun FlatMusicSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    inactiveColor: Color? = null
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var pendingFraction by remember { mutableStateOf<Float?>(null) }

    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val committedFraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)

    LaunchedEffect(valueRange) {
        pendingFraction = null
    }
    LaunchedEffect(committedFraction, pendingFraction) {
        val pending = pendingFraction ?: return@LaunchedEffect
        if (kotlin.math.abs(committedFraction - pending) <= 0.015f) {
            pendingFraction = null
        }
    }

    val fraction = when {
        isDragging -> dragFraction
        pendingFraction != null -> pendingFraction!!
        else -> committedFraction
    }
    val trackColor = inactiveColor ?: MaterialTheme.colorScheme.surfaceVariant
    val resolvedActiveColor = activeColor ?: MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(valueRange) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val startX = 4.dp.toPx()
                    val endX = (size.width - 4.dp.toPx()).coerceAtLeast(startX)
                    val updateFromX: (Float) -> Unit = { x ->
                        val nextFraction = ((x - startX) / (endX - startX).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        dragFraction = nextFraction
                    }

                    pendingFraction = null
                    isDragging = true
                    updateFromX(down.position.x)
                    drag(down.id) { change ->
                        updateFromX(change.position.x)
                        change.consume()
                    }
                    val committedValue = valueRange.start + dragFraction * range
                    pendingFraction = dragFraction
                    onValueChange(committedValue)
                    isDragging = false
                }
            }
    ) {
        val centerY = size.height / 2f
        val strokeWidth = 8.dp.toPx()
        val startX = 4.dp.toPx()
        val endX = (size.width - 4.dp.toPx()).coerceAtLeast(startX)
        val stopRadius = strokeWidth / 2f
        val stopCenterX = endX
        val progressX = startX + (endX - startX) * fraction
        val inactiveStartX = (progressX + 12.dp.toPx()).coerceAtMost(endX)

        if (fraction > 0f) {
            drawLine(
                color = resolvedActiveColor,
                start = Offset(startX, centerY),
                end = Offset(max(progressX, startX), centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        if (inactiveStartX < endX) {
            drawLine(
                color = trackColor,
                start = Offset(inactiveStartX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // The terminal stop is the same color, diameter, and round-cap radius
        // as the inactive track, so it reads as its natural end rather than a
        // pasted-on thumb.
        drawCircle(
            color = trackColor,
            radius = stopRadius,
            center = Offset(stopCenterX, centerY)
        )
    }
}
