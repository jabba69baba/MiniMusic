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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A slim fast-scroll index pinned to the right edge, similar to the Contacts-app
 * alphabet strip. Dragging (or tapping) a letter scrolls [onLetterSelected] and pops
 * up a large bubble showing the letter currently under the finger.
 *
 * [letters] should be the distinct, sorted set of section letters actually present
 * in the list (so users never land on an empty letter).
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
            },
        contentAlignment = Alignment.Center
    ) {
        // The thin letter strip itself.
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEachIndexed { index, letter ->
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDragging && index == activeIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Floating bubble showing the letter under the finger while dragging.
        if (isDragging) {
            val bubbleOffsetFraction = (activeIndex + 0.5f) / letters.size
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = -64.dp.roundToPx(),
                            y = (trackHeightPx * bubbleOffsetFraction).roundToInt() - 32.dp.roundToPx()
                        )
                    }
                    .size(56.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 4.dp, bottomStart = 28.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letters[activeIndex].toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun Modifier.onSizeChangedHeight(onHeight: (Float) -> Unit): Modifier =
    this.then(
        androidx.compose.ui.layout.onGloballyPositioned { coordinates ->
            onHeight(coordinates.size.height.toFloat())
        }
    )
