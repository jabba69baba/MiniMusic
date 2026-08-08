package com.example.minimusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A flat, solid-fill seek bar in the Apple Music / Spotify style: a rounded
 * track, a filled active portion in [activeColor], and a small circular thumb
 * at the current position. Replaces the earlier wavy slider, which turned out
 * to be a maintenance headache for comparatively little payoff — this is a
 * simpler, more standard control. `value`/`valueRange` behave like the
 * standard Slider; tapping or dragging anywhere on the track seeks to that
 * position.
 */
@Composable
fun FlatMusicSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color? = null
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val committedFraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val fraction = if (isDragging) dragFraction else committedFraction

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val resolvedActiveColor = activeColor ?: MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newFraction * range)
                }
            }
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onValueChange(valueRange.start + dragFraction * range)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        val midY = size.height / 2f
        val activeWidth = size.width * fraction
        val strokeWidth = 6.dp.toPx()
        val thumbRadius = 8.dp.toPx()

        drawLine(
            color = trackColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        if (activeWidth > 0f) {
            drawLine(
                color = resolvedActiveColor,
                start = Offset(0f, midY),
                end = Offset(activeWidth, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = resolvedActiveColor,
            radius = thumbRadius,
            center = Offset(activeWidth, midY)
        )
    }
}
