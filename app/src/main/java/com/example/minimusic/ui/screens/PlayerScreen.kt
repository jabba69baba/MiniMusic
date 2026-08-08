package com.example.minimusic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.AudioFormatInfo
import com.example.minimusic.data.model.Song
import com.example.minimusic.data.readAudioFormatInfo
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.components.WavyMusicSlider
import com.example.minimusic.ui.theme.rememberArtAccentColor
import com.example.minimusic.ui.viewmodel.LyricsState
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit

private enum class PlayerPanel { NOW_PLAYING, LYRICS }

/** Large rounded-square corner radius used for the album art frame. */
private val ArtCornerShape = RoundedCornerShape(28.dp)

/** Rounded-square (not full circle) used only on the primary play/pause button. */
private val PlayButtonShape = RoundedCornerShape(20.dp)

/** Preset durations offered in the sleep timer menu. */
private val SleepTimerPresetsMinutes = listOf(5, 15, 30, 45, 60)

/**
 * Player screen. Header: chevron-down collapse (left), centered "Now Playing"
 * label, sleep timer button (right). Large rounded album art; wavy seek bar with
 * a circular thumb and an audio-format badge when the file exposes that info;
 * a transport row of two circular buttons flanking a rounded-square play/pause;
 * a single enclosed capsule (not separate pills) holding shuffle/repeat/lyrics,
 * spanning the same width as the transport row above it; and a slim "Queue" bar
 * pinned at the bottom that expands in place to show the upcoming tracks.
 *
 * The screen tints itself with a hue pulled from the current song's album art
 * (see ArtColor.kt) — the rest of the app stays on the system Material You
 * palette from Theme.kt.
 */
@Composable
fun PlayerScreen(
    playbackState: PlaybackUiState,
    lyricsState: LyricsState,
    showLyricsInitially: Boolean,
    sleepTimerState: SleepTimerState? = null,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onStartSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    var panel by remember(song.id) {
        mutableStateOf(if (showLyricsInitially) PlayerPanel.LYRICS else PlayerPanel.NOW_PLAYING)
    }
    var queueExpanded by remember { mutableStateOf(false) }
    val accent = rememberArtAccentColor(song.albumArtUri)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        // Header: chevron-down (left) — "Now Playing" (centered) — sleep timer (right).
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
            }
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
            SleepTimerButton(
                sleepTimerState = sleepTimerState,
                onStart = onStartSleepTimer,
                onCancel = onCancelSleepTimer,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Main content grows to fill available space; queue bar stays pinned below it.
        Box(modifier = Modifier.weight(1f)) {
            when (panel) {
                PlayerPanel.NOW_PLAYING -> NowPlayingPanel(
                    song = song,
                    playbackState = playbackState,
                    accent = accent,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onSkipPrevious = onSkipPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onCycleRepeat = onCycleRepeat,
                    onOpenLyrics = { panel = PlayerPanel.LYRICS }
                )
                PlayerPanel.LYRICS -> LyricsPanel(
                    song = song,
                    lyricsState = lyricsState,
                    onBackToPlayer = { panel = PlayerPanel.NOW_PLAYING }
                )
            }
        }

        // Slim "Queue" bar pinned at the bottom — expands in place to show upcoming tracks.
        if (playbackState.queue.size > 1) {
            QueueBar(
                playbackState = playbackState,
                accent = accent,
                expanded = queueExpanded,
                onToggleExpanded = { queueExpanded = !queueExpanded },
                onQueueItemClick = onQueueItemClick
            )
        }
    }
}

@Composable
private fun SleepTimerButton(
    sleepTimerState: SleepTimerState?,
    onStart: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (sleepTimerState != null) {
            // Active timer: tapping shows remaining time and offers to cancel.
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { menuExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = "Sleep timer active",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatRemaining(sleepTimerState.remainingMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.Timer, contentDescription = "Sleep timer")
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (sleepTimerState != null) {
                DropdownMenuItem(
                    text = { Text("Cancel timer") },
                    onClick = { onCancel(); menuExpanded = false }
                )
            } else {
                SleepTimerPresetsMinutes.forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text("$minutes min") },
                        onClick = { onStart(minutes * 60_000L); menuExpanded = false }
                    )
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(ms.coerceAtLeast(0L))
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L)) % 60
    return if (totalMinutes > 0) "${totalMinutes}m" else "${seconds}s"
}

@Composable
private fun NowPlayingPanel(
    song: Song,
    playbackState: PlaybackUiState,
    accent: Color,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val context = LocalContext.current
    var formatInfo by remember(song.id) { mutableStateOf<AudioFormatInfo?>(null) }

    LaunchedEffect(song.id) {
        formatInfo = readAudioFormatInfo(context, song.contentUri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .aspectRatio(1f)
                .clip(ArtCornerShape)
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
                    .clip(ArtCornerShape)
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
                text = song.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            WavyMusicSlider(
                value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                isPlaying = playbackState.isPlaying,
                onValueChange = { onSeekTo(it.toLong()) },
                activeColor = accent,
                showThumb = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDuration(playbackState.positionMs), style = MaterialTheme.typography.labelMedium)

                val badgeText = formatInfo?.toBadgeText()
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelMedium)
            }
        }

        // Transport row: circular previous / rounded-square play-pause / circular next.
        // Its total width is the reference the capsule row below matches.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onSkipPrevious
            )
            TransportButton(
                icon = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                shape = PlayButtonShape,
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                sizeMultiplier = 1.15f,
                onClick = onTogglePlayPause
            )
            TransportButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next",
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onSkipNext
            )
        }

        // Single enclosed capsule (one continuous Surface, internally divided) holding
        // shuffle/repeat/lyrics — fillMaxWidth so it spans the same total width as the
        // transport row above, rather than three separate pill buttons with gaps.
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                CapsuleSegment(
                    icon = Icons.Filled.Shuffle,
                    active = playbackState.isShuffled,
                    contentDescription = "Shuffle",
                    onClick = onToggleShuffle,
                    modifier = Modifier.weight(1f)
                )
                CapsuleSegment(
                    icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    active = playbackState.repeatMode != RepeatMode.OFF,
                    contentDescription = "Repeat",
                    onClick = onCycleRepeat,
                    modifier = Modifier.weight(1f)
                )
                CapsuleSegment(
                    icon = Icons.Filled.Subtitles,
                    active = false,
                    contentDescription = "Lyrics",
                    onClick = onOpenLyrics,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    sizeMultiplier: Float = 1f
) {
    val baseSize = 72.dp
    Box(
        modifier = Modifier
            .size(baseSize * sizeMultiplier)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.fillMaxSize(0.45f)
        )
    }
}

/** One segment of the enclosed capsule row — no individual background/border, just an icon with an active tint. */
@Composable
private fun CapsuleSegment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QueueBar(
    playbackState: PlaybackUiState,
    accent: Color,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onQueueItemClick: (Int) -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220))
        ) {
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Queue",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, end = 4.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Collapse queue" else "Expand queue",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@Composable
private fun LyricsPanel(song: Song, lyricsState: LyricsState, onBackToPlayer: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onBackToPlayer) {
                Icon(Icons.Filled.MusicNote, contentDescription = "Back to player")
            }
        }

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

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
