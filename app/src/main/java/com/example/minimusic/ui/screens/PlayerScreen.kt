package com.example.minimusic.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.minimusic.data.AudioFormatInfo
import com.example.minimusic.data.model.Song
import com.example.minimusic.data.readAudioFormatInfo
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.components.FlatMusicSlider
import com.example.minimusic.ui.components.QueueDrawer
import com.example.minimusic.ui.components.QueueDrawerCollapsedHeight
import com.example.minimusic.ui.theme.ArtColorRoles
import com.example.minimusic.ui.theme.rememberArtColorRoles
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit

/** Large rounded-square corner radius used for the album art frame. */
private val ArtCornerShape = RoundedCornerShape(10.dp)

/** 80dp-high play/pause shape with semicircular left and right ends and flat center edges. */
private val PlayButtonShape = RoundedCornerShape(percent = 50)

/** Corner radius each capsule segment takes on when it becomes active. */
private val ActiveSegmentShape = RoundedCornerShape(50)

/** Fixed height shared by all three transport controls. */
private val TransportButtonSize = 72.dp

/** Circular previous/next controls matching the play/pause control height. */
private val TransportCircleSize = 72.dp

/** Original function capsule height. */
private val CapsuleSegmentHeight = 48.dp

/** Gap from album art to the title/artist block and from title/artist to seekbar. */
private val ContentSectionGap = 16.dp

/** Gap from the seekbar to the timer/audio-quality row. */
private val SeekbarToTimeGap = 6.dp

/** Restored control-to-control spacing requested for the lower PlayerScreen. */
private val ControlSectionGap = 20.dp

/** Gap between the transport row and the function capsule. */
private val FunctionSectionGap = 26.dp

/** Extra reserved clearance before the bottom-anchored queue drawer. */
private val CapsuleToQueueGap = 23.dp

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
    showLyricsInitially: Boolean,
    showAudioQualityBadge: Boolean = true,
    sleepTimerState: SleepTimerState? = null,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onStartSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    var autoOpenedLyrics by remember(song.id) { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    val artColors = rememberArtColorRoles(song.albumArtUri)
    val view = LocalView.current

    val visibleNavigationSurface = if (queueOpen) artColors.surfaceVariant else artColors.background
    DisposableEffect(view, artColors.background, visibleNavigationSurface) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = artColors.background.toArgb()
            window.navigationBarColor = visibleNavigationSurface.toArgb()
            controller.isAppearanceLightStatusBars = artColors.background.luminance() > 0.52f
            controller.isAppearanceLightNavigationBars = visibleNavigationSurface.luminance() > 0.52f
        }
        onDispose { }
    }

    LaunchedEffect(song.id, showLyricsInitially) {
        if (showLyricsInitially && !autoOpenedLyrics) {
            autoOpenedLyrics = true
            onOpenLyrics()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(artColors.background)
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
                .padding(
                    bottom = if (playbackState.queue.size > 1) {
                        QueueDrawerCollapsedHeight + CapsuleToQueueGap
                    } else {
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
                    }
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = artColors.onBackground
                    )
                }
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = artColors.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )
                SleepTimerButton(
                    sleepTimerState = sleepTimerState,
                    onStart = onStartSleepTimer,
                    onCancel = onCancelSleepTimer,
                    artColors = artColors,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NowPlayingPanel(
                    song = song,
                    playbackState = playbackState,
                    artColors = artColors,
                    showAudioQualityBadge = showAudioQualityBadge,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onSkipPrevious = onSkipPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onCycleRepeat = onCycleRepeat,
                    onOpenLyrics = onOpenLyrics
                )
            }
        }

        // Draggable queue drawer overlay — Auxio-style: album art and header
        // above stay put, the drawer slides up over the lower portion of the
        // screen rather than a separate full-screen modal.
        if (playbackState.queue.size > 1) {
            QueueDrawer(
                queue = playbackState.queue,
                currentIndex = playbackState.currentIndex,
                artColors = artColors,
                isOpen = queueOpen,
                onOpenChange = { queueOpen = it },
                onSongClick = onQueueItemClick
            )
        }
    }
}

@Composable
private fun SleepTimerButton(
    sleepTimerState: SleepTimerState?,
    onStart: (Long) -> Unit,
    onCancel: () -> Unit,
    artColors: ArtColorRoles,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (sleepTimerState != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = artColors.primaryContainer,
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
                        tint = artColors.onPrimaryContainer
                    )
                    Text(
                        text = formatRemaining(sleepTimerState.remainingMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = artColors.onPrimaryContainer
                    )
                }
            }
        } else {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.Timer, contentDescription = "Sleep timer", tint = artColors.onBackground)
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
    artColors: ArtColorRoles,
    showAudioQualityBadge: Boolean,
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
                .padding(horizontal = 4.dp)
                .padding(top = 4.dp)
                .aspectRatio(1f)
                .clip(ArtCornerShape)
                .background(artColors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.3f),
                tint = artColors.onPrimaryContainer
            )
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(240, delayMillis = 40)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(280))) togetherWith
                        fadeOut(animationSpec = tween(180))
                },
                label = "albumArtTransition"
            ) { displayedSong ->
                AsyncImage(
                    model = displayedSong.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(ArtCornerShape)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .padding(top = ContentSectionGap)
        ) {
            Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = artColors.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 900,
                        repeatDelayMillis = 1_100,
                        velocity = 34.dp
                    )
                )

            Text(
                text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = artColors.onSurfaceVariant,
                    maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .padding(top = ContentSectionGap)
        ) {
            FlatMusicSlider(
                value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                onValueChange = { onSeekTo(it.toLong()) },
                activeColor = artColors.primary,
                inactiveColor = artColors.onSurface.copy(alpha = 0.34f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .padding(top = SeekbarToTimeGap),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatDuration(playbackState.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = artColors.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val badgeText = formatInfo?.toBadgeText()
                    if (showAudioQualityBadge && badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = artColors.surfaceVariant
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = artColors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = artColors.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TransportButtonSize)
                .padding(top = ControlSectionGap),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                shape = CircleShape,
                containerColor = artColors.secondaryContainer,
                contentColor = artColors.onSecondaryContainer,
                onClick = onSkipPrevious,
                modifier = Modifier.requiredSize(TransportCircleSize)
            )
            PlayPauseButton(
                isPlaying = playbackState.isPlaying,
                containerColor = artColors.primary,
                contentColor = artColors.onPrimary,
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .weight(1f)
                    .requiredHeight(TransportButtonSize)
            )
            TransportButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next",
                shape = CircleShape,
                containerColor = artColors.secondaryContainer,
                contentColor = artColors.onSecondaryContainer,
                onClick = onSkipNext,
                modifier = Modifier.requiredSize(TransportCircleSize)
            )
        }

        // Outer capsule: one continuous background. Each segment is flush with
        // it while inactive; when active, that segment gets its own filled
        // rounded-pill highlight that visually pops out from the shared
        // background — independently, so more than one can be active at once.
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = FunctionSectionGap)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = artColors.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CapsuleSegmentHeight)
            ) {
                Row(modifier = Modifier.padding(4.dp).fillMaxHeight()) {
                    CapsuleSegment(
                        artColors = artColors,
                        icon = Icons.Filled.Shuffle,
                        active = playbackState.isShuffled,
                        contentDescription = "Shuffle",
                        onClick = onToggleShuffle,
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleSegment(
                        artColors = artColors,
                        icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        active = playbackState.repeatMode != RepeatMode.OFF,
                        contentDescription = "Repeat",
                        onClick = onCycleRepeat,
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleSegment(
                        artColors = artColors,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.20f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 70 else 180),
        label = "transportPressIllumination"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = pressOverlayAlpha))
        )
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
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(PlayButtonShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(animationSpec = tween(170)) +
                    androidx.compose.animation.scaleIn(initialScale = 0.72f, animationSpec = tween(220))) togetherWith
                    (androidx.compose.animation.fadeOut(animationSpec = tween(120)) +
                        androidx.compose.animation.scaleOut(targetScale = 0.72f, animationSpec = tween(150)))
            },
            label = "playPauseMorph"
        ) { playing ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (playing) "Pause" else "Play",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
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
    artColors: ArtColorRoles,
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
                if (active) artColors.primaryContainer
                else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) artColors.onPrimaryContainer else artColors.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
