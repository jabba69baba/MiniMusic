package com.example.minimusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val TWO_PI_F = (2.0 * PI).toFloat()

/**
 * A seek bar whose active (played) portion draws as a gentle traveling sine wave
 * while music is playing, and flattens to a straight line while paused or while
 * the user is actively dragging to seek. `value`/`valueRange` behave like the
 * standard Slider; tapping or dragging anywhere on the track seeks to that
 * position. Optionally draws a solid circular thumb at the current position
 * (matching a filled-dot seek-bar reference design) — off by default so it
 * doesn't change the look anywhere this component was already in use.
 */
@Composable
fun WavyMusicSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color? = null,
    showThumb: Boolean = false
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val committedFraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val fraction = if (isDragging) dragFraction else committedFraction

    val infiniteTransition = rememberInfiniteTransition(label = "wavy_slider_phase")
    val phase: Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI_F,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1400, easing = LinearEasing)),
        label = "wavy_slider_phase_value"
    )
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 1f else 0f,
        label = "wavy_slider_amplitude"
    )

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
        val maxAmplitudePx = 5.dp.toPx()
        val waveLength = (size.width / 6f).coerceAtLeast(1f)

        if (activeWidth < size.width) {
            drawLine(
                color = trackColor,
                start = Offset(activeWidth, midY),
                end = Offset(size.width, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        var lastActiveY = midY
        if (activeWidth > 0f) {
            val path = Path()
            val step = 4f
            var x = 0f
            var first = true
            while (x <= activeWidth) {
                val y = midY + amplitude * maxAmplitudePx * sin((TWO_PI_F * x / waveLength) + phase)
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }
            val endY = midY + amplitude * maxAmplitudePx * sin((TWO_PI_F * activeWidth / waveLength) + phase)
            path.lineTo(activeWidth, endY)
            lastActiveY = endY

            drawPath(
                path = path,
                color = resolvedActiveColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        if (showThumb) {
            val thumbRadius = 8.dp.toPx()
            drawCircle(
                color = resolvedActiveColor,
                radius = thumbRadius,
                center = Offset(activeWidth, lastActiveY)
            )
        }
    }
}
