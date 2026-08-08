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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.example.minimusic.ui.components.FlatMusicSlider
import com.example.minimusic.ui.theme.rememberArtAccentColor
import com.example.minimusic.ui.viewmodel.LyricsState
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit

private enum class PlayerPanel { NOW_PLAYING, LYRICS }

/** Large rounded-square corner radius used for the album art frame. */
private val ArtCornerShape = RoundedCornerShape(28.dp)

/** Play/pause button corner radius — noticeably rounded, not a near-square. */
private val PlayButtonShape = RoundedCornerShape(36.dp)

/** Corner radius each capsule segment takes on when it becomes active. */
private val ActiveSegmentShape = RoundedCornerShape(50)

/** Preset durations offered in the sleep timer menu. */
private val SleepTimerPresetsMinutes = listOf(5, 15, 30, 45, 60)

/**
 * Player screen. Header: chevron-down collapse (left), centered "Now Playing"
 * label, sleep timer button (right). Large rounded album art; a flat solid-fill
 * seek bar (Apple Music style) with a circular thumb and an audio-format badge
 * when the file exposes that info; a transport row of two circular buttons
 * flanking a rounded play/pause; a single outer capsule holding shuffle/repeat/
 * lyrics where each segment is flush with the shared background until active,
 * at which point it gets its own filled rounded-pill highlight; and a slim
 * "Queue" bar pinned at the bottom that expands in place.
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
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
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
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
            FlatMusicSlider(
                value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                onValueChange = { onSeekTo(it.toLong()) },
                activeColor = accent
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            PlayPauseButton(
                isPlaying = playbackState.isPlaying,
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onTogglePlayPause,
                modifier = Modifier.weight(1f)
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

        // Outer capsule: one continuous background. Each segment is flush with
        // it while inactive; when active, that segment gets its own filled
        // rounded-pill highlight that visually pops out from the shared
        // background — independently, so more than one can be active at once.
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
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

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(PlayButtonShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = if (isPlaying) "Pause" else "Play",
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * One segment of the outer capsule row. Inactive: transparent, flush with the
 * shared capsule background. Active: gets its own filled rounded-pill
 * background that pops out visually — independent of the other segments, so
 * shuffle and lyrics (for example) can both show as active simultaneously.
 */
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
            .padding(horizontal = 2.dp)
            .clip(ActiveSegmentShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
      
