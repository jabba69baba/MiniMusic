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
        // Auxio-style swipeable cover: drag horizontally to skip prev/next,
        // release past the commit threshold to trigger the skip. The art
        // itself moves with the drag as visual feedback, resetting when the
        // song (and therefore the remember key) changes.
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
                            dragOffsetPx
