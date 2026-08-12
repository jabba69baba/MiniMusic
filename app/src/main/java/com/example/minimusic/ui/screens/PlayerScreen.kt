package com.example.minimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.minimusic.ui.components.QueueDrawer
import com.example.minimusic.ui.components.QueueDrawerCollapsedHeight
import com.example.minimusic.ui.theme.rememberArtAccentColor
import com.example.minimusic.ui.viewmodel.LyricsState
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit

private enum class PlayerPanel { NOW_PLAYING, LYRICS }

/** Large rounded-square corner radius used for the album art frame. */
private val ArtCornerShape = RoundedCornerShape(10.dp)

/** Play/pause button shape — a true stadium pill: corner radius is always
 *  exactly half its own height, so it looks correct regardless of TransportButtonSize. */
private val PlayButtonShape = RoundedCornerShape(percent = 50)

/** Corner radius each capsule segment takes on when it becomes active. */
private val ActiveSegmentShape = RoundedCornerShape(50)

/** Fixed height for the transport row's circular buttons and the play/pause pill — never allowed to shrink. */
private val TransportButtonSize = 96.dp

/** Fixed height for each capsule segment (shuffle/repeat/lyrics) — independent of song-info content above. */
private val CapsuleSegmentHeight = 56.dp

/** Equal vertical gap used between each of the panel's major sections (art block,
 *  transport row, action capsule) so the layout reads as evenly spaced/symmetrical
 *  rather than leaving unconstrained slack for Compose to distribute unevenly. */
private val SectionGap = 20.dp

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
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onStartSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    var panel by remember(song.id) {
        mutableStateOf(if (showLyricsInitially) PlayerPanel.LYRICS else PlayerPanel.NOW_PLAYING)
    }
    var queueOpen by remember { mutableStateOf(false) }
    val accent = rememberArtAccentColor(song.albumArtUri)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 12.dp)
                // Reserve real space for the drawer's collapsed bar sitting on top
                // as a separate overlay below — it isn't part of this Column's
                // layout flow, so padding on the last child here has no effect on
                // the gap before it; this Column has to stop short itself instead.
                .padding(bottom = if (playbackState.queue.size > 1) QueueDrawerCollapsedHeight + SectionGap else 0.dp)
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
        }

        // Draggable queue drawer overlay — Auxio-style: album art and header
        // above stay put, the drawer slides up over the lower portion of the
        // screen rather than a separate full-screen modal.
        if (playbackState.queue.size > 1) {
            QueueDrawer(
                queue = playbackState.queue,
                currentIndex = playbackState.currentIndex,
                accent = accent,
                isOpen = queueOpen,
                onOpenChange = { queueOpen = it },
                onSongClick = onQueueItemClick,
                onMove = onMoveQueueItem
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
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

        Column(modifier = Modifier.padding(top = 18.dp)) {
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

        Column(modifier = Modifier.padding(top = 12.dp)) {
            FlatMusicSlider(
                value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                onValueChange = { onSeekTo(it.toLong()) },
                activeColor = accent
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .padding(top = 12.dp),
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
                .height(TransportButtonSize)
                .padding(top = SectionGap),
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
        Column(modifier = Modifier.padding(top = SectionGap)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CapsuleSegmentHeight)
            ) {
                Row(modifier = Modifier.padding(4.dp).fillMaxHeight()) {
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
            .fillMaxHeight()
            // Width is locked to height (not a fixed size), so this stays a
            // perfect circle no matter how the row's available width changes —
            // only ever adjusts horizontally, never distorts vertically.
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
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
            .fillMaxHeight()
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
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .clip(ActiveSegmentShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick),
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
