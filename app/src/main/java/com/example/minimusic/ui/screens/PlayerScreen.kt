package com.example.minimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.theme.PillShape
import com.example.minimusic.ui.theme.cookieShape
import com.example.minimusic.ui.theme.expressiveBlobShape
import com.example.minimusic.ui.viewmodel.LyricsState
import java.util.concurrent.TimeUnit

private enum class PlayerPanel { NOW_PLAYING, LYRICS }

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
    onQueueItemClick: (Int) -> Unit
) {
    val song = playbackState.currentSong ?: return
    var panel by remember(song.id) {
        mutableStateOf(if (showLyricsInitially) PlayerPanel.LYRICS else PlayerPanel.NOW_PLAYING)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            PanelToggle(current = panel, onSelect = { panel = it })
        }

        when (panel) {
            PlayerPanel.NOW_PLAYING -> NowPlayingPanel(
                song = song,
                playbackState = playbackState,
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
                    color = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
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

@Composable
private fun NowPlayingPanel(
    song: Song,
    playbackState: PlaybackUiState,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueItemClick: (Int) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .aspectRatio(1f)
                .clip(expressiveBlobShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
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
                    .clip(expressiveBlobShape())
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
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(playbackState.positionMs), style = MaterialTheme.typography.labelMedium)
                Text(formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelMedium)
            }
        }

        // Control cluster — a single pill-shaped tonal surface housing every transport
        // control, with a scalloped "cookie" shape morph on the primary play button.
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleGlyphButton(
                    icon = Icons.Filled.Shuffle,
                    active = playbackState.isShuffled,
                    contentDescription = "Shuffle",
                    onClick = onToggleShuffle
                )
                IconButton(onClick = onSkipPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.fillMaxSize(0.55f))
                }
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(cookieShape(petals = 10, amplitude = 0.10f))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize(0.45f)
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.fillMaxSize(0.55f))
                }
                ToggleGlyphButton(
                    icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    active = playbackState.repeatMode != RepeatMode.OFF,
                    contentDescription = "Repeat",
                    onClick = onCycleRepeat
                )
            }
        }

        if (playbackState.queue.size > 1) {
            Text(
                text = "Up next",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(playbackState.queue.withIndex().toList(), key = { it.value.id }) { (index, queuedSong) ->
                    QueueRow(
                        song = queuedSong,
                        isCurrent = index == playbackState.currentIndex,
                        onClick = { onQueueItemClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleGlyphButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(if (active) cookieShape(petals = 8, amplitude = 0.16f) else PillShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    text = "MiniMusic reads lyrics straight from the file's own tag (ID3 USLT) — add them with a tag editor to see them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, horizontal = 16.dp)
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
private fun QueueRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
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
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
