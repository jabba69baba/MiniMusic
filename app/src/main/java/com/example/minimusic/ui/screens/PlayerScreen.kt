package com.example.minimusic.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.AudioFormatInfo
import com.example.minimusic.data.model.Song
import com.example.minimusic.data.readAudioFormatInfo
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.QueueSnapshot
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.components.FlatMusicSlider
import com.example.minimusic.ui.components.LandscapeQueueContent
import com.example.minimusic.ui.components.QueueDrawer
import com.example.minimusic.ui.components.QueueDrawerCollapsedHeight
import com.example.minimusic.ui.theme.ArtColorRoles
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.lerpTo
import com.example.minimusic.ui.theme.rememberArtColorRoles
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

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
private val CapsuleSegmentHeight = 52.dp

/** Gap from album art to the title/artist block and from title/artist to seekbar. */
private val ContentSectionGap = 16.dp

/** Gap from the seekbar to the timer/audio-quality row. */
private val SeekbarToTimeGap = 6.dp

/** Shared duration for coordinated artwork and metadata transitions. */
private const val TrackTransitionDurationMillis = 480

/** Short delay while the incoming audio metadata settles before its badge appears. */
private const val QualityBadgeDelayMillis = 140L

/** Restored control-to-control spacing requested for the lower PlayerScreen. */
private val ControlSectionGap = 20.dp

/** Gap between the transport row and the function capsule. */
private val FunctionSectionGap = 26.dp

/** Extra reserved clearance before the bottom-anchored queue drawer. */
private val CapsuleToQueueGap = 19.dp

/** Preset durations offered in the sleep timer menu. */
private val SleepTimerPresetsMinutes = listOf(5, 10, 15, 20, 30, 45, 60)

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
    queueSnapshot: QueueSnapshot,
    showAudioQualityBadge: Boolean = true,
    centeredTitle: Boolean = false,
    sleepTimerState: SleepTimerState? = null,
    onBack: () -> Unit,
    onSwipeToMiniplayer: () -> Unit = onBack,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onQueueEntryClick: (Long) -> Unit = {},
    onReorderQueue: (Long, Int) -> Unit = { _, _ -> },
    onRemoveQueueEntry: (Long) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onStartSleepTimer: (Long, Boolean) -> Unit = { _, _ -> },
    onCancelSleepTimer: () -> Unit = {},
    onQueueOpenChange: (Boolean) -> Unit = {}
) {
    val song = playbackState.currentSong
    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }
    var queueOpen by remember { mutableStateOf(false) }
    fun setQueueOpen(open: Boolean) {
        queueOpen = open
        onQueueOpenChange(open)
    }
    BackHandler(enabled = queueOpen) {
        setQueueOpen(false)
    }
    val targetArtColors = rememberArtColorRoles(song.albumArtUri)
    val artColors = animateArtColorRoles(targetArtColors)
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val queueSlotVisible = playbackState.queue.size > 1 || playbackState.repeatMode == RepeatMode.ONE
    val view = LocalView.current

    val visibleNavigationSurface = if (queueOpen) artColors.surfaceVariant else artColors.background
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = artColors.background.toArgb()
            window.navigationBarColor = visibleNavigationSurface.toArgb()
            controller.isAppearanceLightStatusBars = artColors.background.luminance() > 0.52f
            controller.isAppearanceLightNavigationBars = visibleNavigationSurface.luminance() > 0.52f
        }
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(artColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val isLandscape = maxWidth > maxHeight && maxHeight >= 320.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                // When a queue exists, the conditional bottom reservation below
                // owns the entire lower clearance so the function-to-queue gap is
                // exactly CapsuleToQueueGap. Without a queue, retain the existing
                // 12dp outer bottom padding.
                .padding(
                    top = if (isLandscape) 0.dp else 4.dp,
                    bottom = if (!isLandscape && !queueSlotVisible) 12.dp else 0.dp
                )
                // Reserve real space for the drawer's collapsed bar sitting on top
                // as a separate overlay below — it isn't part of this Column's
                // layout flow, so padding on the last child here has no effect on
                // the gap before it; this Column has to stop short itself instead.
                .padding(
                    bottom = if (!isLandscape && queueSlotVisible) {
                        QueueDrawerCollapsedHeight + CapsuleToQueueGap + navigationBarInset
                    } else if (isLandscape) {
                        // Landscape has no queue surface or header. Keep only a
                        // small navigation clearance and return the rest to the
                        // two-column player.
                        navigationBarInset + 4.dp
                    } else {
                        navigationBarInset + 12.dp
                    }
                )
        ) {
            @Composable
            fun PlayerHeader(modifier: Modifier) {
                Box(modifier = modifier.fillMaxWidth()) {
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
            }
            if (!isLandscape) {
                PlayerHeader(modifier = Modifier.fillMaxWidth())
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                NowPlayingPanel(
                    song = song,
                    playbackState = playbackState,
                    artColors = artColors,
                    showAudioQualityBadge = showAudioQualityBadge,
                    centeredTitle = centeredTitle,
                    queueOpen = queueOpen,
                    landscapeQueueVisible = queueSlotVisible,
                    balanceAlbumArtSpacing = navigationBarInset > 0.dp,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onSkipPrevious = onSkipPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onCycleRepeat = onCycleRepeat,
                    onOpenLyrics = onOpenLyrics,
                    onSwipeToMiniplayer = onSwipeToMiniplayer,
                    landscapeQueueContent = { open, queueModifier ->
                        LandscapeQueueContent(
                            modifier = queueModifier,
                            snapshot = queueSnapshot,
                            artColors = artColors,
                            isOpen = open,
                            onOpenChange = ::setQueueOpen,
                            onEntryClick = onQueueEntryClick,
                            onReorderEntry = onReorderQueue,
                            onRemoveEntry = onRemoveQueueEntry,
                            onClearQueue = onClearQueue
                        )
                    },
                    isLandscape = isLandscape,
                    modifier = if (isLandscape) Modifier.fillMaxSize() else Modifier
                )
            }
        }

        if (queueSlotVisible && !isLandscape) {
            QueueDrawer(
                snapshot = queueSnapshot,
                artColors = artColors,
                isOpen = queueOpen,
                onOpenChange = ::setQueueOpen,
                onEntryClick = onQueueEntryClick,
                onReorderEntry = onReorderQueue,
                onRemoveEntry = onRemoveQueueEntry,
                onClearQueue = onClearQueue
            )
        }
    }
}

@Composable
private fun SleepTimerButton(
    sleepTimerState: SleepTimerState?,
    onStart: (Long, Boolean) -> Unit,
    onCancel: () -> Unit,
    artColors: ArtColorRoles,
    modifier: Modifier = Modifier
) {
    var dialogOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (sleepTimerState != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = artColors.primaryContainer,
                modifier = Modifier.clickable { dialogOpen = true }
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
            IconButton(onClick = { dialogOpen = true }) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = "Sleep timer",
                    tint = artColors.onBackground
                )
            }
        }

        if (dialogOpen) {
            SleepTimerDialog(
                activeTimer = sleepTimerState,
                onDismiss = { dialogOpen = false },
                onStart = { durationMs, waitUntilSongEnd ->
                    onStart(durationMs, waitUntilSongEnd)
                    dialogOpen = false
                },
                onCancel = {
                    onCancel()
                    dialogOpen = false
                },
                artColors = artColors
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerDialog(
    activeTimer: SleepTimerState?,
    onDismiss: () -> Unit,
    onStart: (Long, Boolean) -> Unit,
    onCancel: () -> Unit,
    artColors: ArtColorRoles
) {
    var selectedIndex by remember {
        mutableStateOf(
            activeTimer?.totalMs?.let { totalMs ->
                SleepTimerPresetsMinutes.indices.minByOrNull { index ->
                    kotlin.math.abs(SleepTimerPresetsMinutes[index] * 60_000L - totalMs)
                }
            } ?: 0
        )
    }
    var endOfCurrentSong by remember { mutableStateOf(activeTimer?.endOfCurrentSong == true) }
    var waitUntilSongEnd by remember { mutableStateOf(activeTimer?.waitUntilSongEnd == true) }
    val selectedMinutes = SleepTimerPresetsMinutes[selectedIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = artColors.surface,
        titleContentColor = artColors.onSurface,
        textContentColor = artColors.onSurfaceVariant,
        title = { Text("Sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (endOfCurrentSong) "End of current song" else "$selectedMinutes minutes",
                    style = MaterialTheme.typography.titleMedium,
                    color = artColors.onSurface
                )
                Slider(
                    modifier = Modifier.height(32.dp),
                    value = selectedIndex.toFloat(),
                    onValueChange = { value ->
                        selectedIndex = value.roundToInt().coerceIn(SleepTimerPresetsMinutes.indices)
                        endOfCurrentSong = false
                    },
                    valueRange = 0f..(SleepTimerPresetsMinutes.lastIndex.toFloat()),
                    steps = SleepTimerPresetsMinutes.size - 2,
                    enabled = !endOfCurrentSong,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(28.dp)
                                .background(artColors.primary, RoundedCornerShape(50))
                        )
                    },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = artColors.primary,
                        activeTrackColor = artColors.primary,
                        inactiveTrackColor = artColors.surfaceVariant,
                        activeTickColor = artColors.onPrimary,
                        inactiveTickColor = artColors.onSurfaceVariant
                    )
                )
                SleepTimerSwitchRow(
                    label = "End of current song",
                    checked = endOfCurrentSong,
                    onCheckedChange = { enabled ->
                        endOfCurrentSong = enabled
                        if (enabled) waitUntilSongEnd = false
                    },
                    artColors = artColors
                )
                SleepTimerSwitchRow(
                    label = "Wait for song to end",
                    checked = waitUntilSongEnd,
                    onCheckedChange = { enabled ->
                        waitUntilSongEnd = enabled
                        if (enabled) endOfCurrentSong = false
                    },
                    artColors = artColors
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = artColors.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = onCancel,
                        enabled = activeTimer != null
                    ) {
                        Text("Cancel timer", color = if (activeTimer != null) artColors.primary else artColors.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            if (endOfCurrentSong) {
                                onStart(0L, true)
                            } else {
                                onStart(selectedMinutes * 60_000L, waitUntilSongEnd)
                            }
                        }
                    ) {
                        Text("Set", color = artColors.primary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun SleepTimerSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    artColors: ArtColorRoles
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (checked) artColors.primaryContainer else artColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (checked) artColors.onPrimaryContainer else artColors.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = artColors.onPrimaryContainer,
                    checkedTrackColor = artColors.primary,
                    uncheckedThumbColor = artColors.onSurfaceVariant,
                    uncheckedTrackColor = artColors.surface
                )
            )
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun preloadAlbumArt(context: Context, song: Song) {
    val artworkUri = song.albumArtUri ?: return
    val request = ImageRequest.Builder(context)
        .data(artworkUri)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
    context.imageLoader.enqueue(request)
}

@Composable
private fun NowPlayingPanel(
    song: Song,
    playbackState: PlaybackUiState,
    artColors: ArtColorRoles,
    showAudioQualityBadge: Boolean,
    centeredTitle: Boolean,
    queueOpen: Boolean,
    landscapeQueueVisible: Boolean,
    balanceAlbumArtSpacing: Boolean,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onSwipeToMiniplayer: () -> Unit,
    landscapeQueueContent: @Composable (Boolean, Modifier) -> Unit = { _, _ -> },
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var formatInfo by remember(song.id) { mutableStateOf<AudioFormatInfo?>(null) }
    var badgeReady by remember(song.id) { mutableStateOf(false) }
    val badgeAlpha = remember { Animatable(0f) }
    var displayedArtworkSong by remember { mutableStateOf(song) }
    var transitionDirection by remember { mutableStateOf(1) }
    val latestSong by rememberUpdatedState(song)

    LaunchedEffect(song.id) {
        badgeReady = false
        badgeAlpha.snapTo(0f)

        coroutineScope {
            val formatJob = async(Dispatchers.IO) {
                readAudioFormatInfo(context, song.contentUri)
            }
            val artworkJob = async(Dispatchers.IO) {
                val artworkUri = song.albumArtUri
                if (artworkUri != null) {
                    val request = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                    context.imageLoader.execute(request)
                }
            }

            if (latestSong.id == song.id) {
                // Artwork is released to the carousel independently of the badge,
                // so metadata extraction cannot delay the visual track switch.
                artworkJob.await()
                if (latestSong.id == song.id) {
                    displayedArtworkSong = song
                }
            } else {
                artworkJob.cancel()
            }

            formatInfo = formatJob.await()
        }

        delay(QualityBadgeDelayMillis)
        if (latestSong.id == song.id) {
            badgeReady = true
            badgeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = MiniMusicMotion.trackChangeExitEffects()
            )
        }
    }

    LaunchedEffect(playbackState.queue) {
        playbackState.queue
            .drop((playbackState.currentIndex + 1).coerceAtLeast(1))
            .take(2)
            .forEach { preloadAlbumArt(context, it) }
    }

    val landscapeTransportButtonSize = if (isLandscape) 64.dp else TransportButtonSize
    val landscapeTransportCircleSize = if (isLandscape) 64.dp else TransportCircleSize
    val landscapeCapsuleHeight = if (isLandscape) 44.dp else CapsuleSegmentHeight
    val landscapeControlSectionGap = if (isLandscape) 18.dp else ControlSectionGap
    val landscapeFunctionSectionGap = if (isLandscape) 18.dp else FunctionSectionGap

    val artworkBlock: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .clip(ArtCornerShape)
                .background(artColors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = displayedArtworkSong,
                transitionSpec = {
                    val direction = transitionDirection
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> direction * fullWidth },
                        animationSpec = MiniMusicMotion.trackChangeEffects()
                    ) + fadeIn(
                        animationSpec = MiniMusicMotion.trackChangeEffects()
                    )) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -direction * fullWidth },
                            animationSpec = MiniMusicMotion.trackChangeEffects()
                        ) + fadeOut(
                            animationSpec = MiniMusicMotion.trackChangeExitEffects()
                        ))
                },
                contentKey = { it.id },
                label = "albumArtCarouselTransition"
            ) { displayedSong ->
                if (displayedSong.albumArtUri == null) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.3f),
                        tint = artColors.onPrimaryContainer
                    )
                } else {
                    AsyncImage(
                        model = displayedSong.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ArtCornerShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    val belowArtworkBlock: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    val direction = transitionDirection
                    (slideInHorizontally(
                        initialOffsetX = { width -> direction * (width / 8) },
                        animationSpec = MiniMusicMotion.trackChangeEffects()
                    ) + fadeIn(
                        animationSpec = MiniMusicMotion.trackChangeEffects()
                    )) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { width -> -direction * (width / 8) },
                            animationSpec = MiniMusicMotion.trackChangeEffects()
                        ) + fadeOut(
                            animationSpec = MiniMusicMotion.trackChangeExitEffects()
                        ))
                },
                contentKey = { it.id },
                label = "songMetadataTransition"
            ) { displayedSong ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .padding(top = if (isLandscape) 0.dp else ContentSectionGap)
                ) {
                    Text(
                        text = displayedSong.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = artColors.onBackground,
                        textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1_000,
                            repeatDelayMillis = 1_400,
                            velocity = 24.dp
                        )
                    )

                    Text(
                        text = displayedSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = artColors.onSurfaceVariant,
                        textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                    inactiveColor = artColors.onSurface.copy(alpha = 0.34f),
                    transitionKey = song.id
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
                        if (showAudioQualityBadge && badgeReady && badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = artColors.surfaceVariant,
                                modifier = Modifier.graphicsLayer { alpha = badgeAlpha.value }
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = artColors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        .height(landscapeTransportButtonSize)
                        .padding(top = landscapeControlSectionGap),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    shape = CircleShape,
                    containerColor = artColors.secondaryContainer,
                    contentColor = artColors.onSecondaryContainer,
                    onClick = {
                        transitionDirection = -1
                        onSkipPrevious()
                    },
                    modifier = Modifier.requiredSize(landscapeTransportCircleSize)
                )
                PlayPauseButton(
                    isPlaying = playbackState.isPlaying,
                    containerColor = artColors.primary,
                    contentColor = artColors.onPrimary,
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .weight(1f)
                        .requiredHeight(landscapeTransportButtonSize)
                )
                TransportButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    shape = CircleShape,
                    containerColor = artColors.secondaryContainer,
                    contentColor = artColors.onSecondaryContainer,
                    onClick = {
                        transitionDirection = 1
                        onSkipNext()
                    },
                    modifier = Modifier.requiredSize(landscapeTransportCircleSize)
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = landscapeFunctionSectionGap)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = artColors.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(landscapeCapsuleHeight)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp).fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CapsuleSegment(
                            artColors = artColors,
                            icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            active = playbackState.repeatMode != RepeatMode.OFF,
                            contentDescription = "Repeat",
                            onClick = onCycleRepeat,
                            isFirst = true,
                            modifier = Modifier.weight(1f)
                        )
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
                            icon = Icons.Filled.Subtitles,
                            active = false,
                            contentDescription = "Lyrics",
                            onClick = onOpenLyrics,
                            isLast = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (isLandscape) {
        PixelPlayerLandscapeContent(
            modifier = modifier,
            albumCoverSection = { artworkModifier -> artworkBlock(artworkModifier) },
            controlsSection = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        belowArtworkBlock(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(landscapeFunctionSectionGap))
                        if (landscapeQueueVisible && !queueOpen) {
                            landscapeQueueContent(
                                false,
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .height(72.dp)
                            )
                        }
                    }
                    if (landscapeQueueVisible && queueOpen) {
                        landscapeQueueContent(
                            true,
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(if (balanceAlbumArtSpacing) Modifier.fillMaxHeight() else Modifier)
        ) {
            if (balanceAlbumArtSpacing) {
                Spacer(modifier = Modifier.weight(1f))
            }

            artworkBlock(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(top = 4.dp)
                    .aspectRatio(1f)
            )

            if (balanceAlbumArtSpacing) {
                Spacer(modifier = Modifier.weight(1f))
            }

            belowArtworkBlock(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PixelPlayerLandscapeContent(
    modifier: Modifier = Modifier,
    albumCoverSection: @Composable (Modifier) -> Unit,
    controlsSection: @Composable () -> Unit
) {
    // Adapted from PixelPlayerOSS FullPlayerLandscapeContent (GPLv3).
    // MiniMusic-specific state, artwork, controls, and callbacks remain local.
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        albumCoverSection(
            Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )
        Spacer(Modifier.width(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            controlsSection()
        }
    }
}

@Composable
private fun animateArtColorRoles(target: ArtColorRoles): ArtColorRoles {
    val progress = remember { Animatable(1f) }
    var fromRoles by remember { mutableStateOf(target) }
    var toRoles by remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        if (toRoles == target) return@LaunchedEffect
        fromRoles = fromRoles.lerpTo(toRoles, progress.value)
        toRoles = target
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = MiniMusicMotion.trackChangeEffects()
        )
    }

    return fromRoles.lerpTo(toRoles, progress.value)
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
        animationSpec = if (isPressed) {
            MiniMusicMotion.fastEffects()
        } else {
            MiniMusicMotion.defaultEffects()
        },
        label = "transportPressIllumination"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) MiniMusicMotion.fastEffects() else MiniMusicMotion.defaultEffects(),
        label = "transportPressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
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
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.20f else 0f,
        animationSpec = if (isPressed) {
            MiniMusicMotion.fastEffects()
        } else {
            MiniMusicMotion.defaultEffects()
        },
        label = "playPausePressIllumination"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = if (isPressed) MiniMusicMotion.fastEffects() else MiniMusicMotion.defaultEffects(),
        label = "playPausePressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .fillMaxHeight()
            .clip(PlayButtonShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(animationSpec = MiniMusicMotion.fastEffects()) +
                        androidx.compose.animation.scaleIn(
                            initialScale = 0.72f,
                            animationSpec = MiniMusicMotion.selectionEffects()
                        )) togetherWith
                        (androidx.compose.animation.fadeOut(animationSpec = MiniMusicMotion.fastEffects()) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.72f,
                                animationSpec = MiniMusicMotion.fastEffects()
                            ))
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = pressOverlayAlpha))
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
    artColors: ArtColorRoles,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (active) artColors.primaryContainer else Color.Transparent,
        animationSpec = MiniMusicMotion.defaultEffects(),
        label = "functionTabBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) artColors.onPrimaryContainer else artColors.onSurfaceVariant,
        animationSpec = MiniMusicMotion.defaultEffects(),
        label = "functionTabContent"
    )
    val inset by animateDpAsState(
        targetValue = if (active) 2.dp else 0.dp,
        animationSpec = MiniMusicMotion.defaultEffects(),
        label = "functionTabInset"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.20f else 0f,
        animationSpec = if (isPressed) MiniMusicMotion.fastEffects() else MiniMusicMotion.defaultEffects(),
        label = "functionTabPressIllumination"
    )
    val density = LocalDensity.current
    val tabScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            active -> 1.03f
            else -> 1f
        },
        animationSpec = when {
            isPressed -> MiniMusicMotion.fastEffects()
            active -> MiniMusicMotion.selectionEffects()
            else -> MiniMusicMotion.defaultEffects()
        },
        label = "functionTabScale"
    )
    val neighborNudge by animateDpAsState(
        targetValue = when {
            !active -> 0.dp
            isFirst -> 1.dp
            isLast -> (-1).dp
            else -> 0.dp
        },
        animationSpec = MiniMusicMotion.selectionEffects(),
        label = "functionTabNeighborNudge"
    )
    // Keep every inner tab pill-shaped. The animated inset and background
    // provide the active transition without exposing rectangular tab corners.
    val tabShape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = tabScale
                scaleY = tabScale
                translationX = with(density) { neighborNudge.toPx() }
            }
            .padding(horizontal = inset)
            .clip(tabShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = pressOverlayAlpha))
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
