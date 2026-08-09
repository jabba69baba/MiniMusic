package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.launch

/**
 * Full-screen drag-up queue sheet, in the style of Auxio's queue drawer: opens as a
 * modal bottom sheet covering the whole player (not squeezed into leftover space),
 * shows every track with a small thumbnail, and supports drag-to-reorder via a
 * handle on each row.
 *
 * Reordering works by long-pressing the drag handle and dragging vertically; the
 * dragged row visually follows the finger, and [onMove] fires live as it crosses
 * a neighboring row's midpoint, matching standard reorderable-list behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    accent: Color,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            ReorderableQueueList(
                queue = queue,
                currentIndex = currentIndex,
                accent = accent,
                onSongClick = { index ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    onSongClick(index)
                },
                onMove = onMove
            )
        }
    }
}

private const val ROW_HEIGHT_DP = 64

@Composable
private fun ReorderableQueueList(
    queue: List<Song>,
    currentIndex: Int,
    accent: Color,
    onSongClick: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT_DP.dp.toPx() }
    val listState = rememberLazyListState()

    // Index currently being dragged, and its live vertical offset in px while dragging.
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
            val isDraggingThis = draggingIndex == index
            QueueSheetRow(
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
                            // Crossed a full row height — commit the move and reset the
                            // offset so the next crossing starts from zero again.
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
private fun QueueSheetRow(
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

        // Drag handle: long-press and drag vertically to reorder this track within
        // the queue. A plain tap on the row (outside this handle) plays it instead.
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
