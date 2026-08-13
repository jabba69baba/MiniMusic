package com.example.minimusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A Material-style fast-scroll thumb pinned to the right edge — a plain
 * thicker track with a pill-shaped handle, no permanently-visible letters.
 *
 * The letter "sticker" (a rounded burst/cookie shape) pops up beside the
 * thumb whenever [isListScrolling] is true (i.e. the caller's list is being
 * actively scrolled, by any means — flinging, dragging the list itself, or
 * dragging this scrollbar), showing [currentLetter]. It disappears the
 * moment scrolling stops. Dragging or tapping this scrollbar's track also
 * scrolls the list via [onLetterSelected].
 *
 * [letters] should be the distinct, sorted set of section letters actually
 * present in the list (so users never land on an empty letter) — used only
 * to map drag position to a section, not rendered directly.
 */
@Composable
fun AlphabetScrollbar(
    letters: List<Char>,
    currentLetter: Char?,
    isListScrolling: Boolean,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return

    var isDraggingThumb by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableStateOf(0) }
    var trackHeightPx by remember { mutableStateOf(0f) }

    fun updateFromOffsetY(y: Float) {
        if (trackHeightPx <= 0f) return
        val fraction = (y / trackHeightPx).coerceIn(0f, 0.9999f)
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
        if (index != dragIndex || !isDraggingThumb) {
            dragIndex = index
            onLetterSelected(letters[index])
        }
    }

    // While the caller's list is scrolling by any means (fling, drag, or this
    // thumb), show the bubble at the position matching currentLetter; while
    // actively dragging this thumb specifically, show the drag position instead.
    val showBubble = isDraggingThumb || isListScrolling
    val bubbleIndex = if (isDraggingThumb) {
        dragIndex
    } else {
        currentLetter?.let { letters.indexOf(it).takeIf { i -> i >= 0 } } ?: dragIndex
    }
    val thumbFraction = (bubbleIndex + 0.5f) / letters.size

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .onSizeChangedHeight { trackHeightPx = it }
            .pointerInput(letters) {
                detectDragGestures(
                    onDragStart = { offset -> isDraggingThumb = true; updateFromOffsetY(offset.y) },
                    onDragEnd = { isDraggingThumb = false },
                    onDragCancel = { isDraggingThumb = false }
                ) { change, _ -> updateFromOffsetY(change.position.y) }
            }
            .pointerInput(letters) {
                detectTapGestures(onPress = { offset ->
                    isDraggingThumb = true
                    updateFromOffsetY(offset.y)
                    tryAwaitRelease()
                    isDraggingThumb = false
                })
            }
    ) {
        // Thicker plain track, centered in the touch target — no letters shown at rest.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        // Pill-shaped thumb that tracks scroll position, Material fast-scroll style.
        val thumbHeight = 44.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (trackHeightPx * thumbFraction).roundToInt() - thumbHeight.roundToPx() / 2
                    )
                }
                .size(width = 6.dp, height = thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        // Burst/cookie-shaped letter sticker beside the thumb, shown while scrolling.
        if (showBubble && bubbleIndex in letters.indices) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -60.dp.roundToPx(),
                            y = (trackHeightPx * thumbFraction).roundToInt() - 32.dp.roundToPx()
                        )
                    }
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                BurstSticker(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = letters[bubbleIndex].toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * A rounded 10-point "cookie/sticker" burst shape, matching M3 Expressive's
 * playful shape language, drawn manually rather than via the experimental
 * MaterialShapes API so it doesn't depend on a bleeding-edge Material3 version.
 */
@Composable
private fun BurstSticker(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val points = 10
        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius * 0.80f
        val center = Offset(size.width / 2f, size.height / 2f)

        fun pointAt(index: Int, radius: Float): Offset {
            val angle = (Math.PI * index / points) - Math.PI / 2
            return Offset(
                center.x + (radius * cos(angle)).toFloat(),
                center.y + (radius * sin(angle)).toFloat()
            )
        }

        // Alternates outer (bump tip) and inner (valley) vertices, but connects
        // them with quadratic curves through the valley points rather than
        // straight lines — that's what turns a sharp star into the soft,
        // rounded-bump "cookie" shape in the reference image.
        val path = Path().apply {
            val firstOuter = pointAt(0, outerRadius)
            moveTo(firstOuter.x, firstOuter.y)
            for (i in 0 until points) {
                val valley = pointAt(2 * i + 1, innerRadius)
                val nextOuter = pointAt(2 * i + 2, outerRadius)
                quadraticTo(valley.x, valley.y, nextOuter.x, nextOuter.y)
            }
            close()
            fillType = PathFillType.NonZero
        }
        drawPath(path, color = color, style = Fill)
    }
}

private fun Modifier.onSizeChangedHeight(onHeight: (Float) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates ->
        onHeight(coordinates.size.height.toFloat())
    }
