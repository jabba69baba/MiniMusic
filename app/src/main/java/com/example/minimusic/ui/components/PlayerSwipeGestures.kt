package com.example.minimusic.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Minimum upward drag distance (px) on the mini player before it's treated
 *  as an intentional "open full player" swipe rather than an accidental
 *  wobble or a tap. */
private const val OpenDragThresholdPx = 60f

/** Minimum downward drag distance (px) on the full player before it's
 *  treated as an intentional "return to mini player" swipe. */
private const val DismissDragThresholdPx = 80f

/**
 * Attach to the mini player: a clear upward swipe opens the full player,
 * same destination as tapping it. Small/accidental movements below the
 * threshold are ignored so normal taps and slight jitter don't trigger it.
 */
fun Modifier.miniPlayerSwipeUpModifier(onOpenPlayer: () -> Unit): Modifier = this.pointerInput(Unit) {
    var accumulatedDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { accumulatedDrag = 0f },
        onDragEnd = { accumulatedDrag = 0f },
        onDragCancel = { accumulatedDrag = 0f },
        onVerticalDrag = { change, dragAmount ->
            accumulatedDrag += dragAmount
            if (accumulatedDrag <= -OpenDragThresholdPx) {
                change.consume()
                onOpenPlayer()
                accumulatedDrag = 0f
            }
        }
    )
}

/**
 * Attach to the full player screen's outermost container: a clear downward
 * swipe returns to the mini player (pops back), mirroring the swipe-up that
 * opened it. Ignores small movements so scrolling/dragging within the
 * screen's own content (e.g. the queue drawer, lyrics list) isn't hijacked
 * unless the gesture is unambiguously a top-level vertical swipe.
 */
fun Modifier.playerSwipeDownDismissModifier(onDismiss: () -> Unit): Modifier = this.pointerInput(Unit) {
    var accumulatedDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { accumulatedDrag = 0f },
        onDragEnd = { accumulatedDrag = 0f },
        onDragCancel = { accumulatedDrag = 0f },
        onVerticalDrag = { change, dragAmount ->
            accumulatedDrag += dragAmount
            if (accumulatedDrag >= DismissDragThresholdPx) {
                change.consume()
                onDismiss()
                accumulatedDrag = 0f
            }
        }
    )
}
