package com.example.minimusic.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A plain Material-style fast-scroll thumb pinned to the right edge — just a
 * thicker track with a pill-shaped handle that tracks scroll position. No
 * popup, no letters shown anywhere on this component; dragging or tapping
 * the track scrolls the list via [onLetterSelected].
 *
 * The caller is responsible for sizing this to exactly match the scrollable
 * list's own bounds (same height, same vertical position) — this component
 * simply fills whatever height its modifier gives it.
 *
 * [letters] should be the distinct, sorted set of section letters actually
 * present in the list (so users never land on an empty letter) — used only
 * to map drag position to a section, never rendered.
 */
@Composable
fun AlphabetScrollbar(
    letters: List<Char>,
    currentLetter: Char?,
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

    // The thumb always tracks real scroll position via currentLetter (kept in
    // sync with the list by the caller), except mid-drag where it follows the
    // finger directly for immediate, glitch-free feedback.
    val thumbIndex = if (isDraggingThumb) {
        dragIndex
    } else {
        currentLetter?.let { letters.indexOf(it).takeIf { i -> i >= 0 } } ?: dragIndex
    }
    val thumbFraction = (thumbIndex + 0.5f) / letters.size

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
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
        // Thicker plain track, centered in the touch target.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(8.dp)
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
                .size(width = 8.dp, height = thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

private fun Modifier.onSizeChangedHeight(onHeight: (Float) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates ->
        onHeight(coordinates.size.height.toFloat())
    }
