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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A flat, solid-fill seek bar in the Apple Music style: a thick rounded (squircle)
 * track, with the played portion drawn as a solid rounded-rect fill that ends in
 * the same rounded shape as the track itself — no separate circular thumb, the
 * fill's own rounded end IS the indicator. `value`/`valueRange` behave like the
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
            .height(14.dp)
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
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        val activeWidth = (size.width * fraction).coerceAtLeast(size.height) // never smaller than the round cap itself

        // Full track — squircle/pill shaped, drawn as a single rounded-rect.
        drawPath(
            path = Path().apply {
                addRoundRect(RoundRect(rect = androidx.compose.ui.geometry.Rect(Offset.Zero, Size(size.width, size.height)), cornerRadius = cornerRadius))
            },
            color = trackColor
        )

        // Active (played) portion — its own rounded-rect fill from the start;
        // its rounded right edge is the only "thumb" indicator, no separate dot.
        if (fraction > 0f) {
            drawPath(
                path = Path().apply {
                    addRoundRect(RoundRect(rect = androidx.compose.ui.geometry.Rect(Offset.Zero, Size(activeWidth, size.height)), cornerRadius = cornerRadius))
                },
                color = resolvedActiveColor
            )
        }
    }
}
