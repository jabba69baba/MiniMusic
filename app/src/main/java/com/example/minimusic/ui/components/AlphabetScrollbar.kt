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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A Material-style fast-scroll thumb pinned to the right edge — a plain
 * thicker track with a pill-shaped handle, no permanently-visible letters.
 *
 * A bold-lettered "dewdrop" bubble (a circle with a point aimed at the thumb)
 * pops up beside the thumb only while the user is actively dragging or
 * pressing this scrollbar itself — not while the caller's list is being
 * scrolled by hand or fling, which stays silent. It disappears the moment
 * the drag on this scrollbar ends.
 *
 * [letters] should be the distinct, sorted set of section letters actually
 * present in the list (so users never land on an empty letter) — used only
 * to map drag position to a section, not rendered directly.
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

    // The thumb itself always tracks real scroll position (from currentLetter,
    // kept in sync with the list by the caller) — that part is independent of
    // whether the user is touching the scrollbar. Only the dewdrop bubble is
    // gated to isDraggingThumb, per the reference: it must NOT appear just
    // because the list is being scrolled by hand.
    val thumbIndex = if (isDraggingThumb) {
        dragIndex
    } else {
        currentLetter?.let { letters.indexOf(it).takeIf { i -> i >= 0 } } ?: dragIndex
    }
    val thumbFraction = (thumbIndex + 0.5f) / letters.size

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

        // Dewdrop letter bubble beside the thumb — only while dragging this scrollbar.
        if (isDraggingThumb && thumbIndex in letters.indices) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -56.dp.roundToPx(),
                            y = (trackHeightPx * thumbFraction).roundToInt() - 28.dp.roundToPx()
                        )
                    }
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                DewdropSticker(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = letters[thumbIndex].toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.offset(x = (-6).dp)
                )
            }
        }
    }
}

/**
 * A circular "dewdrop" bubble with a point extending to the right toward the
 * scrollbar thumb — matching the reference (Gramophone-style) fast-scroll
 * indicator, drawn manually so it doesn't depend on any experimental API.
 */
@Composable
private fun DewdropSticker(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val circleRadius = size.minDimension * 0.42f
        val centerX = size.width * 0.40f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)
        val tipX = size.width * 0.98f

        // The point's two base vertices sit on the circle at roughly +/-40°
        // from the horizontal axis facing the tip, so the point blends into
        // the circle's curve rather than sticking out as a sharp triangle.
        val baseAngle = Math.toRadians(38.0)
        val topBase = Offset(
            centerX + (circleRadius * cos(baseAngle)).toFloat(),
            centerY - (circleRadius * sin(baseAngle)).toFloat()
        )
        val bottomBase = Offset(
            centerX + (circleRadius * cos(baseAngle)).toFloat(),
            centerY + (circleRadius * sin(baseAngle)).toFloat()
        )

        val path = Path().apply {
            addOval(Rect(center = center, radius = circleRadius))
            moveTo(topBase.x, topBase.y)
            quadraticTo(centerX + circleRadius * 1.05f, centerY, tipX, centerY)
            quadraticTo(centerX + circleRadius * 1.05f, centerY, bottomBase.x, bottomBase.y)
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
