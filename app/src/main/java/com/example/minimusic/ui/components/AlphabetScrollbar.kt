package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A minimal Material-style fast-scroll thumb pinned to the right edge — a
 * plain thin track with a pill-shaped handle, no permanently-visible letters.
 * Dragging (or tapping) the track scrolls via [onLetterSelected] and pops up
 * a circular bubble to the left of the thumb showing the letter currently
 * under the finger; the bubble disappears once the drag ends.
 *
 * [letters] should be the distinct, sorted set of section letters actually
 * present in the list (so users never land on an empty letter) — used only
 * to map drag position to a section, not rendered directly.
 */
@Composable
fun AlphabetScrollbar(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return

    var isDragging by remember { mutableStateOf(false) }
    var activeIndex by remember { mutableStateOf(0) }
    var trackHeightPx by remember { mutableStateOf(0f) }

    fun updateFromOffsetY(y: Float) {
        if (trackHeightPx <= 0f) return
        val fraction = (y / trackHeightPx).coerceIn(0f, 0.9999f)
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
        if (index != activeIndex || !isDragging) {
            activeIndex = index
            onLetterSelected(letters[index])
        }
    }

    val thumbFraction = (activeIndex + 0.5f) / letters.size

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(vertical = 8.dp)
            .onSizeChangedHeight { trackHeightPx = it }
            .pointerInput(letters) {
                detectDragGestures(
                    onDragStart = { offset -> isDragging = true; updateFromOffsetY(offset.y) },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ -> updateFromOffsetY(change.position.y) }
            }
            .pointerInput(letters) {
                detectTapGestures(onPress = { offset ->
                    isDragging = true
                    updateFromOffsetY(offset.y)
                    tryAwaitRelease()
                    isDragging = false
                })
            }
    ) {
        // Plain thin track, centered in the touch target — no letters shown at rest.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        // Pill-shaped thumb that tracks drag position, Material fast-scroll style.
        val thumbHeight = 40.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (trackHeightPx * thumbFraction).roundToInt() - thumbHeight.roundToPx() / 2
                    )
                }
                .size(width = 4.dp, height = thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        // Floating letter bubble to the left of the thumb, only while dragging.
        if (isDragging) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -56.dp.roundToPx(),
                            y = (trackHeightPx * thumbFraction).roundToInt() - 24.dp.roundToPx()
                        )
                    }
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letters[activeIndex].toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun Modifier.onSizeChangedHeight(onHeight: (Float) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates ->
        onHeight(coordinates.size.height.toFloat())
    }
