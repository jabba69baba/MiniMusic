package com.example.minimusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.theme.PillShape
import com.example.minimusic.ui.theme.atAlpha
import com.example.minimusic.ui.theme.rememberArtAccentColor
import com.example.minimusic.ui.viewmodel.LyricsState
import java.util.concurrent.TimeUnit

private enum class PlayerPanel { NOW_PLAYING, LYRICS }

/**
 * Player screen, restructured around Auxio's playback-screen interaction model:
 * swipe the cover art itself left/right to skip tracks (no separate prev/next
 * gesture area), a flat/no-rounded-corners layout throughout (see Shape.kt),
 * and the queue ("Up next") presented as a collapsible drawer rather than an
 * always-expanded inline list — closer to Auxio's swipe-up queue sheet.
 *
 * The screen's background is tinted with a hue pulled from the current song's
 * album art (see ArtColor.kt); this is the one place in the app that departs
 * from the pure system Material You palette used everywhere else in the app.
 */
@Composable
fun PlayerScreen(
    playbackState: PlaybackUiState,
    lyricsState: LyricsState,
    showLyricsInitially: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onOpenSleepTimer: () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    var panel by remember(song.id) {
        mutableStateOf(if (showLyricsInitially) PlayerPanel.LYRICS else PlayerPanel.NOW_PLAYING)
    }
    var queueExpanded by remember { mutableStateOf(false) }

    val accent = rememberArtAccentColor(song.albumArtUri)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(accent.atAlpha(0.20f), MaterialTheme.colorScheme.surface)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSleepTimer) {
                Icon(Icons.Filled.Timer, contentDescription = "Sleep timer")
            }
            PanelToggle(current = panel, onSelect = { panel = it })
        }

        when (panel) {
            PlayerPanel.NOW_PLAYING -> NowPlayingPanel(
                song = song,
                playbackState = playbackState,
                accent = accent,
                queueExpanded = queueExpanded,
                onToggleQueueExpanded = { queueExpanded = !queueExpanded },
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onSkipPrevious = onSkipPrevious,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onCycleRepeat = onCycleRepeat,
                onQueueItemClick = onQueueItemClick
            )
            PlayerPanel.LYRICS -> LyricsPanel(song = song, lyricsState = lyricsState)
        }
    }
}

@Composable
private fun PanelToggle(current: PlayerPanel, onSelect: (PlayerPanel) -> Unit) {
    Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf(PlayerPanel.NOW_PLAYING to "Player", PlayerPanel.LYRICS to "Lyrics").forEach { (value, label) ->
                val selected = current == value
                Surface(
                    shape = PillShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable { onSelect(value) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Fraction of the art's width a horizontal drag must cross before it commits to a skip. */
private const val SWIPE_COMMIT_FRACTION = 0.28f

@Composable
private fun NowPlayingPanel(
    song: Song,
    playbackState: PlaybackUiState,
    accent: Color,
    queueExpanded: Boolean,
    onToggleQueueExpanded: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueItemClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        var dragOffsetPx by remember(song.id) { mutableFloatStateOf(0f) }
        var artWidthPx by remember { mutableFloatStateOf(1f) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .aspectRatio(1f)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .onSizeChanged { artWidthPx = it.width.toFloat() }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> dragOffsetPx += delta },
                    onDragStopped = {
                        val threshold = artWidthPx * SWIPE_COMMIT_FRACTION
                        when {
                            dragOffsetPx <= -threshold -> onSkipNext()
                            dragOffsetPx >= threshold -> onSkipPrevious()
                        }
                        dragOffsetPx = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.3f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(PillShape)
                    .offset(x = with(density) { dragOffsetPx.toDp() })
            )
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            Slider(
                value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(playbackState.positionMs), style = MaterialTheme.typography.labelMedium)
                Text(formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleGlyphButton(
                icon = Icons.Filled.Shuffle,
                active = playbackState.isShuffled,
                accent = accent,
                contentDescription = "Shuffle",
                onClick = onToggleShuffle
            )
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.fillMaxSize(0.55f))
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accent)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize(0.45f)
                )
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.fillMaxSize(0.55f))
            }
            ToggleGlyphButton(
                icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                active = playbackState.repeatMode != RepeatMode.OFF,
                accent = accent,
                contentDescription = "Repeat",
                onClick = onCycleRepeat
            )
        }

        if (playbackState.queue.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .clickable(onClick = onToggleQueueExpanded),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Up next (${(playbackState.queue.size - playbackState.currentIndex - 1).coerceAtLeast(0)})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                Icon(
                    imageVector = if (queueExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (queueExpanded) "Collapse queue" else "Expand queue"
                )
            }
            AnimatedVisibility(
                visible = queueExpanded,
                enter = expandVertically(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(220))
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(playbackState.queue.withIndex().toList(), key = { it.value.id }) { (index, queuedSong) ->
                        QueueRow(
                            song = queuedSong,
                            isCurrent = index == playbackState.currentIndex,
                            accent = accent,
                            onClick = { onQueueItemClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleGlyphButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}

@Composable
private fun LyricsPanel(song: Song, lyricsState: LyricsState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        when (lyricsState) {
            is LyricsState.Loading, LyricsState.Idle -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is LyricsState.NotFound -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No embedded lyrics found for this track",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MiniMusic reads lyrics straight from the file's own tag (ID3 USLT/SYLT) — add them with a tag editor to see them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            is LyricsState.Found -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = lyricsState.text,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6f
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(song: Song, isCurrent: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
