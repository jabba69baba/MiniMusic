package com.example.minimusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.theme.ArtColorRoles
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

private const val OPEN_FRACTION = 0.82f
val QueueDrawerCollapsedHeight = 48.dp
private const val ROW_HEIGHT_DP = 64

@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    history: List<Song>,
    queue: List<Song>,
    currentIndex: Int,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSongClick: (Long) -> Unit,
    onMoveItem: (Long, Long) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { QueueDrawerCollapsedHeight.toPx() }
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val openOffsetPx = fullHeightPx * (1f - OPEN_FRACTION)
    val closedOffsetPx = fullHeightPx - collapsedBarHeightPx - navBarHeightPx
    val listState = rememberLazyListState()
    val allSongs = remember(history, queue) { history + queue }
    val allSongIds = remember(allSongs) { allSongs.map { it.id } }
    val isListAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    var offsetY by remember { mutableFloatStateOf(closedOffsetPx) }
    var isDraggingSheet by remember { mutableStateOf(false) }
    var sheetDragMoved by remember { mutableStateOf(false) }
    var headerDragMoved by remember { mutableStateOf(false) }
    var animationTarget by remember { mutableStateOf<Float?>(null) }

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
    val headerDragState = rememberDraggableState { delta ->
        headerDragMoved = true
        isDraggingSheet = true
        animationTarget = null
        offsetY = (offsetY + delta).coerceIn(openOffsetPx, closedOffsetPx)
    }
    val dragState = rememberDraggableState { delta ->
        sheetDragMoved = true
        isDraggingSheet = true
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
                .fillMaxSize()
                .draggable(
                    enabled = sheetDragEnabled,
                    orientation = Orientation.Vertical,
                    state = dragState,
                    startDragImmediately = true,
                    onDragStopped = { velocity ->
                        val didDrag = sheetDragMoved
                        sheetDragMoved = false
                        isDraggingSheet = false
                        if (didDrag) {
                            val shouldOpen = if (abs(velocity) > 700f) {
                                velocity < 0f
                            } else {
                                offsetY < (openOffsetPx + closedOffsetPx) / 2f
                            }
                            animationTarget = if (shouldOpen) openOffsetPx else closedOffsetPx
                            onOpenChange(shouldOpen)
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                QueueHeader(
                    artColors = artColors,
                    isOpen = isOpen,
                    onToggle = { onOpenChange(!isOpen) },
                    onHeaderDragStopped = { velocity ->
                        val didDrag = headerDragMoved
                        headerDragMoved = false
                        isDraggingSheet = false
                        if (didDrag) {
                            val shouldOpen = if (!isOpen) {
                                velocity < -700f || offsetY < (openOffsetPx + closedOffsetPx) / 2f
                            } else {
                                velocity < -700f && offsetY <= (openOffsetPx + closedOffsetPx) / 2f
                            }
                            animationTarget = if (shouldOpen) openOffsetPx else closedOffsetPx
                            onOpenChange(shouldOpen)
                        }
                    },
                    headerDragState = headerDragState
                )
                if (isOpen || isDraggingSheet || offsetY < closedOffsetPx - collapsedBarHeightPx / 2f) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = artColors.onSurfaceVariant.copy(alpha = 0.28f)
                    )
                    QueueDrawerList(
                        songs = allSongs,
                        currentSongId = queue.firstOrNull()?.id,
                        allSongIds = allSongIds,
                        artColors = artColors,
                        listState = listState,
                        onSongClick = onSongClick,
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
    onToggle: () -> Unit,
    onHeaderDragStopped: (Float) -> Unit,
    headerDragState: androidx.compose.foundation.gestures.DraggableState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Vertical,
                state = headerDragState,
                startDragImmediately = true,
                onDragStopped = onHeaderDragStopped
            )
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
    songs: List<Song>,
    currentSongId: Long?,
    allSongIds: List<Long>,
    artColors: ArtColorRoles,
    listState: LazyListState,
    onSongClick: (Long) -> Unit,
    onMoveItem: (Long, Long) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    var activeDragId by remember { mutableStateOf<Long?>(null) }
    var activeDragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT_DP.dp.toPx() }

    LaunchedEffect(songs, currentSongId) {
        val currentIndex = songs.indexOfFirst { it.id == currentSongId }
        if (currentIndex >= 0) {
            val currentVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            if (!currentVisible) listState.scrollToItem(currentIndex)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            val isCurrent = song.id == currentSongId
            val isActiveDrag = song.id == activeDragId
            SimpleQueueRow(
                song = song,
                isCurrent = isCurrent,
                artColors = artColors,
                isDragging = isActiveDrag,
                dragOffset = if (isActiveDrag) activeDragOffset else 0f,
                allSongIds = allSongIds,
                onClick = { onSongClick(song.id) },
                onRemove = { onRemoveItem(song.id) },
                onDragStart = {
                    activeDragId = song.id
                    activeDragOffset = 0f
                },
                onDragAmount = { delta ->
                    activeDragId?.let { dragId ->
                        activeDragOffset += delta
                        val fromIndex = allSongIds.indexOf(dragId)
                        val shift = (activeDragOffset / rowHeightPx).roundToInt()
                        val targetIndex = (fromIndex + shift).coerceIn(0, allSongIds.lastIndex)
                        if (fromIndex >= 0 && targetIndex != fromIndex) {
                            onMoveItem(dragId, allSongIds[targetIndex])
                            activeDragOffset -= (targetIndex - fromIndex) * rowHeightPx
                        }
                    }
                },
                onDragEnd = {
                    activeDragId = null
                    activeDragOffset = 0f
                }
            )
        }
    }
}

@Composable
private fun SimpleQueueRow(
    song: Song,
    isCurrent: Boolean,
    artColors: ArtColorRoles,
    isDragging: Boolean,
    dragOffset: Float,
    allSongIds: List<Long>,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragAmount: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val containerColor = if (isCurrent) artColors.primaryContainer else artColors.surface.copy(alpha = 0.72f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT_DP.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SixDotHandle(
                color = artColors.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .pointerInput(song.id, allSongIds) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragCancel = onDragEnd,
                            onDragEnd = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragAmount(dragAmount.y)
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(44.dp)
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
                    .padding(start = 6.dp, end = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = artColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${formatQueueDuration(song.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
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
}

@Composable
private fun SixDotHandle(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawSixDots(color)
    }
}

private fun DrawScope.drawSixDots(color: Color) {
    val radius = 2.2.dp.toPx()
    val xPositions = listOf(size.width * 0.36f, size.width * 0.64f)
    val yPositions = listOf(size.height * 0.26f, size.height * 0.50f, size.height * 0.74f)
    xPositions.forEach { x ->
        yPositions.forEach { y ->
            drawCircle(color = color, radius = radius, center = Offset(x, y))
        }
    }
}

private fun formatQueueDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
