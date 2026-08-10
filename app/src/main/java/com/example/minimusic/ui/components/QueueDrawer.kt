package com.example.minimusic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.launch
import kotlin.math.abs

/** How far up the drawer sits when open, as a fraction of the available height. */
private const val OPEN_FRACTION = 0.82f

/** Height of the always-visible collapsed bar (handle + "Queue" label). */
private val CollapsedBarHeight = 64.dp

private const val ROW_HEIGHT_DP = 64

/**
 * A draggable queue drawer that lives inside the Player screen's own layout,
 * rather than a separate full-screen modal — matching Auxio's behavior: the
 * album art and header stay put above it, and the drawer slides up over the
 * lower portion of the screen. Collapsed, only a slim handle + "Queue" bar
 * shows; dragging it up (or tapping it) reveals the full list underneath.
 *
 * Must be placed inside a [Box] that fills the area the drawer should be able
 * to expand into (typically the whole Player screen content area).
 */
@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    queue: List<Song>,
    currentIndex: Int,
    accent: Color,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSongClick: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { CollapsedBarHeight.toPx() }
    val navBarHeightPx = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(density).toFloat()
    val openOffsetPx = fullHeightPx * (1f - OPEN_FRACTION)
    // The drawer's own Column already pads its content below the nav bar, but the
    // *resting* (closed) position is calculated here against the full screen height
    // before that inner padding applies — so the nav bar height is subtracted
    // explicitly as well, or the collapsed bar visually sits underneath/behind it.
    val closedOffsetPx = fullHeightPx - collapsedBarHeightPx - navBarHeightPx

    val offsetY = remember { Animatable(closedOffsetPx) }
    val scope = rememberCoroutineScope()

    // Keep the drawer's animated position in sync with isOpen when it changes
    // from outside a drag (e.g. tapping the collapsed bar, or a song selection
    // closing it).
    androidx.compose.runtime.LaunchedEffect(isOpen, fullHeightPx) {
        val target = if (isOpen) openOffsetPx else closedOffsetPx
        offsetY.animateTo(target, animationSpec = tween(260))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = offsetY.value }
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newValue = (offsetY.value + delta).coerceIn(openOffsetPx, closedOffsetPx)
                            offsetY.snapTo(newValue)
                        }
                    },
                    onDragStopped = { velocity ->
                        val shouldOpen = if (abs(velocity) > 800f) {
                            velocity < 0f
                        } else {
                            offsetY.value < (openOffsetPx + closedOffsetPx) / 2f
                        }
                        onOpenChange(shouldOpen)
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                // Collapsed bar: drag handle + icon + "Queue" label, always visible
                // at the top of the drawer regardless of open/closed state —
                // tapping it toggles, matching the slim bar in the collapsed
                // reference. Extra bottom padding above keeps it clear of the
                // system navigation bar rather than sitting flush against it.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChange(!isOpen) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 8.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                // Only render the list once the drawer is meaningfully open —
                // this is what actually stops the collapsed state from showing
                // a sliver of the first row peeking out below the handle bar.
                val isSubstantiallyOpen = offsetY.value < closedOffsetPx - collapsedBarHeightPx / 2f
                if (isSubstantiallyOpen) {
                    QueueDrawerList(
                        queue = queue,
                        currentIndex = currentIndex,
                        accent = accent,
                        onSongClick = { index ->
                            onSongClick(index)
                            onOpenChange(false)
                        },
                        onMove = onMove
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueDrawerList(
    queue: List<Song>,
    currentIndex: Int,
    accent: Color,
    onSongClick: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT_DP.dp.toPx() }
    val listState = rememberLazyListState()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
            val isDraggingThis = draggingIndex == index
            QueueDrawerRow(
                song = song,
                isCurrent = index == currentIndex,
                accent = accent,
                rowModifier = Modifier.graphicsLayer {
                    translationY = if (isDraggingThis) dragOffsetPx else 0f
                },
                onClick = { onSongClick(index) },
                dragModifier = Modifier.pointerInput(song.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffsetPx = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetPx += dragAmount.y

                            val from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            val rowsMoved = (dragOffsetPx / rowHeightPx).toInt()
                            if (rowsMoved != 0) {
                                val target = (from + rowsMoved).coerceIn(0, queue.lastIndex)
                                if (target != from) {
                                    onMove(from, target)
                                    draggingIndex = target
                                    dragOffsetPx -= rowsMoved * rowHeightPx
                                }
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffsetPx = 0f
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun QueueDrawerRow(
    song: Song,
    isCurrent: Boolean,
    accent: Color,
    rowModifier: Modifier,
    onClick: () -> Unit,
    dragModifier: Modifier
) {
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .height(ROW_HEIGHT_DP.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .then(dragModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
