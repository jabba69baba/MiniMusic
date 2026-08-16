package com.example.minimusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A fast-scroll thumb pinned to the right edge whose position reflects true
 * scroll progress through [itemCount] items — not which letter section is
 * currently visible. With a large, unevenly-distributed library (e.g. many
 * more songs under "S" than "X"), a letter-indexed thumb sits frozen for
 * long scrolls within one heavy letter and then jumps at the boundary —
 * driving it by raw list position instead makes it move continuously and
 * proportionally no matter how songs are distributed across letters.
 *
 * Dragging or tapping the track scrolls the list via [onScrollToIndex],
 * called with a target item index. A small bubble shows the letter for
 * whatever index the finger is currently over (resolved via
 * [letterForIndex]), the way contact lists / Spotify show a letter preview
 * during a fast-scroll drag on the scrollbar itself — it only appears while
 * the user is directly touching the scrollbar (drag or press-hold on the
 * track/thumb), never from an ordinary swipe on the list content, and
 * lingers briefly after release before fading rather than vanishing the
 * instant the finger lifts.
 *
 * The caller is responsible for sizing this to exactly match the scrollable
 * list's own bounds (same height, same vertical position) — this component
 * simply fills whatever height its modifier gives it.
 */
@Composable
fun AlphabetScrollbar(
    itemCount: Int,
    currentIndex: Int,
    letterForIndex: (Int) -> Char?,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (itemCount <= 0) return

    var isDraggingThumb by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableStateOf(0) }
    var trackHeightPx by remember { mutableStateOf(0f) }

    fun updateFromOffsetY(y: Float) {
        if (trackHeightPx <= 0f) return
        val fraction = (y / trackHeightPx).coerceIn(0f, 1f)
        val index = (fraction * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
        dragIndex = index
        onScrollToIndex(index)
    }

    // The thumb tracks real scroll position via currentIndex (kept in sync
    // with the list by the caller), except mid-drag where it follows the
    // finger directly for immediate, glitch-free feedback.
    val thumbIndex = if (isDraggingThumb) dragIndex else currentIndex
    val thumbFraction = if (itemCount <= 1) 0f else thumbIndex.toFloat() / (itemCount - 1).toFloat()

    // Bubble stays visible for a brief moment after the finger lifts off the
    // scrollbar (matches how Spotify/contact lists linger before fading)
    // rather than disappearing the instant isDraggingThumb flips back to
    // false — but it is driven ONLY by direct interaction with this
    // scrollbar (drag/press on the track or thumb), never by the song
    // list's own scroll state, so an ordinary swipe through the list never
    // triggers it.
    var showBubble by remember { mutableStateOf(false) }
    LaunchedEffect(isDraggingThumb) {
        if (isDraggingThumb) {
            showBubble = true
        } else {
            delay(400)
            showBubble = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .onSizeChangedHeight { trackHeightPx = it }
            .pointerInput(itemCount) {
                detectDragGestures(
                    onDragStart = { offset -> isDraggingThumb = true; updateFromOffsetY(offset.y) },
                    onDragEnd = { isDraggingThumb = false },
                    onDragCancel = { isDraggingThumb = false }
                ) { change, _ -> updateFromOffsetY(change.position.y) }
            }
            .pointerInput(itemCount) {
                detectTapGestures(onPress = { offset ->
                    isDraggingThumb = true
                    updateFromOffsetY(offset.y)
                    tryAwaitRelease()
                    isDraggingThumb = false
                })
            }
    ) {
        // Thinner plain track than the thumb, centered in the touch target —
        // narrower than the thumb on purpose so the thumb visibly sticks out
        // past it on both edges, making the active/inactive split obvious at
        // a glance rather than the two blending into one uniform width.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        // Pill-shaped thumb that tracks scroll position, Material fast-scroll
        // style. Shorter than before (roughly 65% of the old height) so it
        // reads as a proportionally smaller, more precise indicator — with
        // very large libraries the old height made it look like it barely
        // moved as you scrolled. Wider than the track so it visibly stands
        // proud of it on both sides.
        val thumbWidth = 9.dp
        val thumbHeight = 34.dp
        val density = LocalDensity.current
        val thumbOffsetY = remember(trackHeightPx, thumbFraction, thumbHeight, density) {
            val thumbHeightPx = with(density) { thumbHeight.roundToPx() }
            val rawY = (trackHeightPx * thumbFraction).roundToInt() - thumbHeightPx / 2
            rawY.coerceIn(0, (trackHeightPx.roundToInt() - thumbHeightPx).coerceAtLeast(0))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    // Center the thumb on its target position, but clamp so it
                    // never renders above the track's top or past its bottom —
                    // without this, a position near either end minus half the
                    // thumb height can push it outside the track's own bounds.
                    IntOffset(x = 0, y = thumbOffsetY)
                }
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        // Letter bubble: shown only while directly interacting with this
        // scrollbar (drag or press-hold on the track/thumb) — never
        // triggered by scrolling the list content itself. Floats to the
        // left of the thumb with a clear gap so it doesn't crowd the track,
        // at the same vertical position, previewing which letter section is
        // current. Lingers briefly after release rather than vanishing the
        // instant the finger lifts.
        val bubbleSize = 48.dp
        AnimatedVisibility(
            visible = showBubble,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val gap = 16.dp.roundToPx()
                    IntOffset(
                        x = -(thumbWidth.roundToPx() + gap),
                        y = thumbOffsetY - (bubbleSize - thumbHeight).roundToPx() / 2
                    )
                }
        ) {
            val letter = letterForIndex(thumbIndex)
            if (letter != null) {
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun Modifier.onSizeChangedHeight(onHeight: (Float) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates ->
        onHeight(coordinates.size.height.toFloat())
    }
