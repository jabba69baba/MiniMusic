package com.example.minimusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.theme.ArtColorRoles
import kotlin.math.abs
import kotlin.math.roundToInt

/** How far up the drawer sits when open, as a fraction of the available height. */
private const val OPEN_FRACTION = 0.82f

/** Height of the always-visible collapsed bar (handle + Queue label). */
val QueueDrawerCollapsedHeight = 48.dp

private const val ROW_HEIGHT_DP = 72

/**
 * Connected queue sheet with two distinct gesture regions:
 * collapsed: only the visible Queue bar can open it;
 * expanded: the list scrolls normally, and the sheet closes only when the list
 * is already at its first item and the user drags downward.
 */
@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    queue: List<Song>,
    currentIndex: Int,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSongClick: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { QueueDrawerCollapsedHeight.toPx() }
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val openOffsetPx = fullHeightPx * (1f - OPEN_FRACTION)
    val closedOffsetPx = fullHeightPx - collapsedBarHeightPx - navBarHeightPx
    val listState = rememberLazyListState()
    val isListAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    var offsetY by remember { mutableFloatStateOf(closedOffsetPx) }
    var animationTarget by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isOpen, fullHeightPx) {
        animationTarget = if (isOpen) openOffsetPx else closedOffsetPx
    }

    LaunchedEffect(animationTarget) {
        val target = animationTarget ?: return@LaunchedEffect
        val start = offsetY
        animate(
            initialValue = start,
            targetValue = target,
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ) { value, _ -> offsetY = value }
        offsetY = target
        animationTarget = null
    }

    val sheetDragEnabled = !isOpen || isListAtTop
    val dragState = rememberDraggableState { delta ->
        isDragging = true
        animationTarget = null
        offsetY = (offsetY + delta).coerceIn(openOffsetPx, closedOffsetPx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = artColors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .draggable(
                    enabled = sheetDragEnabled,
                    orientation = Orientation.Vertical,
                    state = dragState,
                    startDragImmediately = true,
                    onDragStopped = { velocity ->
                        isDragging = false
                        val shouldOpen = if (abs(velocity) > 700f) {
                            velocity < 0f
                        } else {
                            offsetY < (openOffsetPx + closedOffsetPx) / 2f
                        }
                        animationTarget = if (shouldOpen) openOffsetPx else closedOffsetPx
                        onOpenChange(shouldOpen)
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                QueueHeader(
                    artColors = artColors,
                    isOpen = isOpen,
                    onToggle = { onOpenChange(!isOpen) }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = artColors.onSurfaceVariant.copy(alpha = 0.28f)
                )
                if (isOpen || isDragging || offsetY < closedOffsetPx - collapsedBarHeightPx / 2f) {
                    QueueDrawerList(
                        queue = queue,
                        currentIndex = currentIndex,
                        artColors = artColors,
                        listState = listState,
                        onSongClick = { index ->
                            onSongClick(index)
                            onOpenChange(false)
                        },
                        onMoveItem = onMoveItem,
                        onRemoveItem = onRemoveItem
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 6.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(artColors.onSurfaceVariant.copy(alpha = 0.60f))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = if (isOpen) "Close queue" else "Open queue",
                modifier = Modifier.size(18.dp),
                tint = artColors.onSurface
            )
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                color = artColors.onSurface,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun QueueDrawerList(
    queue: List<Song>,
    currentIndex: Int,
    artColors: ArtColorRoles,
    listState: LazyListState,
    onSongClick: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    var activeDragIndex by remember { mutableStateOf<Int?>(null) }
    var activeDragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT_DP.dp.toPx() }

    LaunchedEffect(queue, currentIndex) {
        if (currentIndex in queue.indices) {
            val currentVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            if (!currentVisible) listState.scrollToItem(currentIndex)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
            val isDragging = activeDragIndex == index
            QueueDrawerRow(
                index = index,
                song = song,
                isCurrent = index == currentIndex,
                artColors = artColors,
                isDragging = isDragging,
                dragOffset = if (isDragging) activeDragOffset else 0f,
                onClick = { onSongClick(index) },
                onRemove = { onRemoveItem(index) },
                onHandleDrag = { dragStartIndex ->
                    activeDragIndex = dragStartIndex
                    activeDragOffset = 0f
                },
                onHandleDragAmount = { _, delta ->
                    activeDragIndex?.let { dragIndex ->
                        activeDragOffset += delta
                        val shift = (activeDragOffset / rowHeightPx).roundToInt()
                        val targetIndex = (dragIndex + shift).coerceIn(0, queue.lastIndex)
                        if (targetIndex != dragIndex) {
                            onMoveItem(dragIndex, targetIndex)
                            activeDragIndex = targetIndex
                            activeDragOffset -= (targetIndex - dragIndex) * rowHeightPx
                        }
                    }
                },
                onHandleDragEnd = {
                    activeDragIndex = null
                    activeDragOffset = 0f
                }
            )
            if (index < queue.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = artColors.onSurfaceVariant.copy(alpha = 0.16f)
                )
            }
        }
    }
}

@Composable
private fun QueueDrawerRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    artColors: ArtColorRoles,
    isDragging: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onHandleDrag: (Int) -> Unit,
    onHandleDragAmount: (Int, Float) -> Unit,
    onHandleDragEnd: () -> Unit
) {
    val containerColor = if (isCurrent) artColors.primaryContainer else artColors.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT_DP.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SixDotHandle(
            color = artColors.onSurfaceVariant,
            modifier = Modifier
                .size(28.dp)
                .pointerInput(song.id, index) {
                    var workingIndex = index
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onHandleDrag(workingIndex)
                        },
                        onDragCancel = onHandleDragEnd,
                        onDragEnd = onHandleDragEnd,
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onHandleDragAmount(workingIndex, dragAmount.y)
                        }
                    )
                }
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(artColors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrent) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = artColors.onSecondaryContainer
            )
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = artColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatQueueDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = artColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ${song.title} from queue",
                tint = artColors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SixDotHandle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawSixDots(color)
    }
}

private fun DrawScope.drawSixDots(color: Color) {
    val radius = 2.4.dp.toPx()
    val xPositions = listOf(size.width * 0.38f, size.width * 0.62f)
    val yPositions = listOf(size.height * 0.28f, size.height * 0.50f, size.height * 0.72f)
    xPositions.forEach { x ->
        yPositions.forEach { y ->
            drawCircle(color = color, radius = radius, center = Offset(x, y))
        }
    }
}

private fun formatQueueDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
