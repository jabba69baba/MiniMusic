package com.example.minimusic.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.AudioFormatInfo
import com.example.minimusic.data.PaletteStyle
import com.example.minimusic.data.model.Song
import com.example.minimusic.data.readAudioFormatInfo
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.QueueSnapshot
import com.example.minimusic.playback.RepeatMode
import com.example.minimusic.ui.components.AlbumArtImage
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.components.performMiniMusicHaptic
import com.example.minimusic.ui.components.FlatMusicSlider
import com.example.minimusic.ui.components.LandscapeQueueContent
import com.example.minimusic.ui.components.LandscapeQueueSlot
import com.example.minimusic.ui.components.MiniMusicImageLoader
import com.example.minimusic.ui.components.QueueDrawer
import com.example.minimusic.ui.components.QueueDrawerCollapsedHeight
import com.example.minimusic.ui.theme.ArtColorRoles
import com.example.minimusic.ui.theme.LocalMiniMusicReducedMotion
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.MiniMusicType
import com.example.minimusic.ui.theme.lerpTo
import com.example.minimusic.ui.theme.rememberArtColorRoles
import com.example.minimusic.ui.viewmodel.SleepTimerState
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

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
    playbackFlow: StateFlow<PlaybackUiState>,
    queueSnapshot: QueueSnapshot,
    showAudioQualityBadge: Boolean = true,
    centeredTitle: Boolean = false,
    albumArtPaletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    artworkShadowEnabled: Boolean = true,
    artworkShadowDp: Int = 6,
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
    // Collected here (not in NavGraph) so the 20 Hz position ticker recomposes
    // only this screen — never the library list underneath it.
    val playbackState by playbackFlow.collectAsState()
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
    val targetArtColors = rememberArtColorRoles(song.albumArtUri, albumArtPaletteStyle)
    val artColors = animateArtColorRoles(targetArtColors)
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val queueSlotVisible = playbackState.queue.size > 1 || playbackState.repeatMode == RepeatMode.ONE
    val view = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current

    val visibleNavigationSurface = if (queueOpen) artColors.surfaceContainer else artColors.background
    // Window attributes are system calls: only re-apply when the resolved
    // colors actually change, never on every position-tick recomposition.
    var lastAppliedBars by remember { mutableStateOf<Pair<Color, Color>?>(null) }
    SideEffect {
        val barsKey = artColors.background to visibleNavigationSurface
        if (lastAppliedBars != barsKey) {
            lastAppliedBars = barsKey
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                window.statusBarColor = artColors.background.toArgb()
                window.navigationBarColor = visibleNavigationSurface.toArgb()
                controller.isAppearanceLightStatusBars = artColors.background.luminance() > 0.52f
                controller.isAppearanceLightNavigationBars = visibleNavigationSurface.luminance() > 0.52f
            }
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
                    IconButton(onClick = {
                        if (hapticsEnabled) view.performMiniMusicHaptic()
                        onBack()
                    }, modifier = Modifier.align(Alignment.CenterStart)) {
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
                    landscapeQueueContent = { slot, queueModifier ->
                        LandscapeQueueContent(
                            modifier = queueModifier,
                            slot = slot,
                            snapshot = queueSnapshot,
                            artColors = artColors,
                            isOpen = queueOpen,
                            onOpenChange = ::setQueueOpen,
                            onEntryClick = onQueueEntryClick,
                            onReorderEntry = onReorderQueue,
                            onRemoveEntry = onRemoveQueueEntry,
                            onClearQueue = onClearQueue
                        )
                    },
                    isLandscape = isLandscape,
                    artworkShadowEnabled = artworkShadowEnabled,
                    artworkShadowDp = artworkShadowDp,
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
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current

    Box(modifier = modifier) {
        if (sleepTimerState != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = artColors.primaryContainer,
                modifier = Modifier.clickable { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); dialogOpen = true }
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
                        // Counts down once a second: tabular figures keep the
                        // digits from resizing the chip around them on every tick.
                        style = MiniMusicType.tabular(MaterialTheme.typography.labelMedium),
                        color = artColors.onPrimaryContainer
                    )
                }
            }
        } else {
            IconButton(onClick = { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); dialogOpen = true }) {
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
    var lastHapticIndex by remember { mutableStateOf<Int?>(null) }
    val selectedMinutes = SleepTimerPresetsMinutes[selectedIndex]
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = artColors.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal, color = artColors.onSurface)
                    Text(if (endOfCurrentSong) "End of current song" else "$selectedMinutes minutes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Normal, color = artColors.onSurface)
                }
                Slider(
                    modifier = Modifier.height(32.dp),
                    value = selectedIndex.toFloat(),
                    onValueChange = { value ->
                        val newIndex = value.roundToInt().coerceIn(SleepTimerPresetsMinutes.indices)
                        if (hapticsEnabled && newIndex != lastHapticIndex) hapticView.performMiniMusicHaptic()
                        lastHapticIndex = newIndex
                        selectedIndex = newIndex
                        endOfCurrentSong = false
                    },
                    valueRange = 0f..(SleepTimerPresetsMinutes.lastIndex.toFloat()),
                    steps = SleepTimerPresetsMinutes.size - 2,
                    enabled = !endOfCurrentSong,
                    thumb = {
                        Box(Modifier.width(8.dp).height(28.dp).background(artColors.primary, RoundedCornerShape(50)))
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
                    label = "End of current song", checked = endOfCurrentSong,
                    onCheckedChange = { enabled -> endOfCurrentSong = enabled; if (enabled) waitUntilSongEnd = false }, artColors = artColors
                )
                SleepTimerSwitchRow(
                    label = "Wait for song to end", checked = waitUntilSongEnd,
                    onCheckedChange = { enabled -> waitUntilSongEnd = enabled; if (enabled) endOfCurrentSong = false }, artColors = artColors
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepTimerAction("Dismiss", Modifier.weight(1f), RoundedCornerShape(8.dp), artColors, artColors.surfaceVariant, artColors.onSurface) {
                        if (hapticsEnabled) hapticView.performMiniMusicHaptic(); onDismiss()
                    }
                    SleepTimerAction("Stop", Modifier.weight(1f), RoundedCornerShape(8.dp), artColors, artColors.secondaryContainer, artColors.onSecondaryContainer) {
                        if (hapticsEnabled) hapticView.performMiniMusicHaptic(); onCancel()
                    }
                    SleepTimerAction("Set", Modifier.weight(1f), RoundedCornerShape(8.dp), artColors, artColors.primary, artColors.onPrimary) {
                        if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                        if (endOfCurrentSong) onStart(0L, true) else onStart(selectedMinutes * 60_000L, waitUntilSongEnd)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerAction(
    label: String,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    artColors: ArtColorRoles,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = modifier.height(44.dp), shape = shape, color = containerColor) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
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
        // Neutral container in both states: the row tinting to the accent
        // color when checked was louder than M3E wants — the switch itself
        // reports state.
        color = artColors.surfaceVariant
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
                color = artColors.onSurface,
                // The row's own container color reports the switch state, so
                // the label stays at its role's weight instead of bolding the
                // body scale to fake the emphasis.
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                // Full color set stated explicitly: in this material3 version
                // the M3E switch's BORDER falls back to SwitchDefaults (the
                // app's global theme) and showed a red hue over the
                // art-derived track. The border follows the album art too.
                // (SwitchColors.copy only exists from material3 alpha28 on.)
                colors = androidx.compose.material3.SwitchColors(
                    checkedThumbColor = artColors.onPrimary,
                    checkedTrackColor = artColors.primary,
                    checkedBorderColor = Color.Transparent,
                    checkedIconColor = artColors.onPrimary,
                    uncheckedThumbColor = artColors.onSurfaceVariant,
                    uncheckedTrackColor = artColors.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent,
                    uncheckedIconColor = artColors.onSurfaceVariant,
                    disabledCheckedThumbColor = artColors.onSurfaceVariant.copy(alpha = 0.3f),
                    disabledCheckedTrackColor = artColors.surfaceVariant,
                    disabledCheckedBorderColor = Color.Transparent,
                    disabledCheckedIconColor = artColors.onSurfaceVariant.copy(alpha = 0.3f),
                    disabledUncheckedThumbColor = artColors.onSurfaceVariant.copy(alpha = 0.3f),
                    disabledUncheckedTrackColor = artColors.surfaceVariant,
                    disabledUncheckedBorderColor = Color.Transparent,
                    disabledUncheckedIconColor = artColors.onSurfaceVariant.copy(alpha = 0.3f)
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
    // Same ImageLoader instance as every display site: otherwise this warms a
    // cache nobody reads.
    MiniMusicImageLoader.get(context).enqueue(request)
}

/**
 * Guard before the playback state is expected to confirm a predicted skip
 * (mirrors PixelPlayer's skip-reconciliation window).
 */
private const val CarouselReconcileGuardMs = 900L

/**
 * M3E film-strip carousel for the album art (horizontal).
 *
 * Every queue occurrence keeps a stable position in the strip — item [k]
 * always sits `(k - progress)` viewports away — and a track change animates
 * the shared [progress] spring onto the new index
 * ([MiniMusicMotion.carouselSpatial], critically damped, the same token as
 * the seekbar rewind, so bar and art settle as one choreography). Adjacent
 * covers always tile the viewport: while one leaves, the next is already
 * sliding in, so a rapid skip burst never exposes a blank slot. Each
 * occurrence is composed exactly once — there is no cached artwork layer
 * behind the strip to duplicate a cover — and the single shared [progress]
 * is the sole owner of the motion, so no frame is ever double-driven.
 *
 * Only the focused item and its two neighbors are composed; the window is
 * recomposed only when [focusedIndex] crosses a boundary, while per-frame
 * progress updates stay in the graphicsLayer draw phase.
 */
@Composable
private fun QueueArtStrip(
    progress: Animatable<Float, *>,
    queue: List<Song>,
    focusedIndex: Int,
    artColors: ArtColorRoles,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val window = remember(focusedIndex, queue.size) {
        if (queue.isEmpty()) {
            IntArray(0)
        } else {
            val lo = (focusedIndex - 1).coerceAtLeast(0)
            val hi = (focusedIndex + 1).coerceAtMost(queue.size - 1)
            IntArray(hi - lo + 1) { lo + it }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewportWidthPx = it.width }
    ) {
        // Wait for the first measured width so the three window items don't
        // all draw at x=0 (stacked) for a frame.
        if (viewportWidthPx > 0) {
            for (index in window) {
                val song = queue[index]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = (index - progress.value) * viewportWidthPx
                        }
                ) {
                    if (song.albumArtUri == null) {
                        // PixelPlayer's placeholder icon tone (0.2 on the
                        // primary-container canvas).
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.3f),
                            tint = artColors.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    } else {
                        val artLoadRequest = remember(song.id, song.albumArtUri) {
                            ImageRequest.Builder(context)
                                .data(song.albumArtUri)
                                .crossfade(false)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }
                        // Placeholder tone behind the image so the frame is
                        // never transparent while the bitmap decodes.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(artColors.surfaceContainer)
                        )
                        AsyncImage(
                            model = artLoadRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

/**
 * M3E film strip for the track title/artist block (vertical) — the same
 * contract as [QueueArtStrip], rotated: each queue occurrence is one block
 * exactly the window height, so the band always shows the outgoing block's
 * bottom and the incoming block's top. The title is never fully gone during a
 * handoff, and the direction stays vertical. Programmatic only: the strip
 * consumes no pointer input, so the sheet's vertical drag is unaffected. The
 * infinite marquee runs on the focused item only.
 */
@Composable
private fun VerticalMetadataStrip(
    progress: Animatable<Float, *>,
    queue: List<Song>,
    focusedIndex: Int,
    centeredTitle: Boolean,
    artColors: ArtColorRoles,
    windowHeight: Dp,
    topPadding: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val windowHeightPx = with(density) { windowHeight.toPx() }
    // The strip is the app's tightest text box, so its roles are chosen to fit
    // it rather than the other way round. Portrait gives it 72dp with a 16dp
    // top padding, which holds headlineSmall (30dp) plus bodyLarge (24dp).
    // Landscape gives it 48dp with no top padding at all, and those two lines
    // need 52dp there — the artist line was being clipped by the strip's own
    // bounds. The supporting line steps down to bodySmall in that box so the
    // pair fits with room to spare (46dp of 48dp).
    val compactStrip = windowHeight < 56.dp
    val titleStyle = MaterialTheme.typography.headlineSmall
    val artistStyle = if (compactStrip) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val window = remember(focusedIndex, queue.size) {
        if (queue.isEmpty()) {
            IntArray(0)
        } else {
            val lo = (focusedIndex - 1).coerceAtLeast(0)
            val hi = (focusedIndex + 1).coerceAtMost(queue.size - 1)
            IntArray(hi - lo + 1) { lo + it }
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        for (index in window) {
            val song = queue[index]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (index - progress.value) * windowHeightPx
                    }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .padding(top = topPadding)
                ) {
                    Text(
                        text = song.title,
                        style = titleStyle,
                        color = artColors.onBackground,
                        textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index == focusedIndex) {
                                    Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 900,
                                        initialDelayMillis = 700,
                                        velocity = 19.dp
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                    Text(
                        text = song.artist,
                        style = artistStyle,
                        // Secondary text on the dynamic background takes the
                        // BACKGROUND's own secondary pairing: onBackground at
                        // reduced alpha. onSurfaceVariant is generated against
                        // the *surface* tone, which on art-tinted backgrounds
                        // can drift outside the guaranteed contrast pair — the
                        // washed-out look. Gramophone does the same (its
                        // "contentColor.copy(alpha = 0.72f)" secondary line).
                        color = artColors.onBackground.copy(alpha = 0.72f),
                        textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
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
    landscapeQueueContent: @Composable (LandscapeQueueSlot, Modifier) -> Unit = { _, _ -> },
    isLandscape: Boolean = false,
    artworkShadowEnabled: Boolean = true,
    artworkShadowDp: Int = 6,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    var formatInfo by remember(song.id) { mutableStateOf<AudioFormatInfo?>(null) }
    var badgeReady by remember(song.id) { mutableStateOf(false) }
    // Badge appear is a scale, not a fade: grows 0.8 -> 1 over the static row.
    val badgeScale = remember { Animatable(0.8f) }
    // ---- Track-change carousel (M3E film strip) ---------------------------
    // One shared spring — [MiniMusicMotion.carouselSpatial], the same token as
    // the seekbar rewind — drives BOTH the art strip (horizontal) and the
    // title/artist strip (vertical) as a film strip over the playback queue.
    // Every queue occurrence keeps a stable position in its strip, so
    // adjacent covers/titles always tile the viewport: a rapid skip retargets
    // the in-flight spring from its current value instead of exiting to a
    // blank slot (the old two-leg single-layer handoff). Each occurrence is
    // composed exactly once — no cached artwork layer to duplicate a cover —
    // and this one Animatable is the sole owner of the motion, so no frame is
    // ever double-driven.
    val carouselProgress = remember { Animatable(0f) }
    // Optimistic index from a skip tap: the strip starts moving before the
    // MediaController round-trip lands (PixelPlayer's pendingCarouselIndex);
    // the reconciliation effect below lets the real state confirm or override.
    var pendingCarouselIndex by remember { mutableStateOf<Int?>(null) }
    val queue = playbackState.queue
    val safeLastIndex = (queue.size - 1).coerceAtLeast(0)
    val targetIndex = (pendingCarouselIndex ?: playbackState.currentIndex).coerceIn(0, safeLastIndex)
    // The discrete strip window follows the rounded progress (per-frame
    // progress itself stays in the graphicsLayer draw phase and must never
    // recompose the strips). It is never reset on queue change: the driver
    // below re-anchors it atomically with the rendered list, and any other
    // reset would expose a one-frame mismatch between list and index.
    var focusedIndex by remember {
        mutableIntStateOf(playbackState.currentIndex.coerceIn(0, safeLastIndex))
    }
    LaunchedEffect(carouselProgress) {
        snapshotFlow { carouselProgress.value.roundToInt() }
            .distinctUntilChanged()
            .collect { focusedIndex = it.coerceIn(0, safeLastIndex) }
    }

    // The queue list the strips actually render. It changes only in the same
    // snapshot as the driver's re-anchored progress, so a queue geometry
    // change (shuffle toggle, reorder, edit) never composes the NEW list at
    // the OLD index for one frame — that stale-frame was the "another song's
    // cover and title for a split second" flash on shuffle.
    var renderedQueue by remember { mutableStateOf(queue) }
    var lastQueue by remember { mutableStateOf<List<Song>?>(null) }
    // With reduced motion on, a track change repositions the strip without
    // travelling across it — the same destination, reached without the sweep.
    val reducedMotion = LocalMiniMusicReducedMotion.current

    LaunchedEffect(targetIndex, queue) {
        if (queue.isEmpty() || targetIndex !in queue.indices) return@LaunchedEffect
        // Defense in depth (no animation change): a mid-shuffle state can
        // transiently pair the new list with an index that does not hold the
        // current song. Anchor the strip on the current song's real position
        // instead, or stay put until a settled state arrives. An active skip
        // prediction keeps its own anchor by design.
        val targetIndex = if (pendingCarouselIndex != null || queue.getOrNull(targetIndex)?.id == song.id) {
            targetIndex
        } else {
            queue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: return@LaunchedEffect
        }
        val target = targetIndex.toFloat()
        val distance = abs(carouselProgress.value - target)
        val queueChanged = lastQueue !== queue
        when {
            // Any geometry change refocuses instantly: sliding across a
            // reordered queue would show covers that no longer follow each
            // other, and the rendered list swaps in this same snapshot as the
            // snap, so the focused cover is never wrong.
            queueChanged -> carouselProgress.snapTo(target)
            // Adjacent travels ride the shared carousel spring; long jumps
            // (shuffle landing far away, wrap) snap instead of flying across
            // the queue.
            distance > 1.5f -> carouselProgress.snapTo(target)
            reducedMotion -> carouselProgress.snapTo(target)
            else -> carouselProgress.animateTo(target, MiniMusicMotion.carouselSpatial())
        }
        if (queueChanged) {
            focusedIndex = targetIndex
            renderedQueue = queue
        }
        lastQueue = queue
    }

    // Reconcile the optimistic index with the real playback state. A
    // confirming state matches the prediction and simply keeps it; a
    // diverged one (repeat edges, a skip the controller declined) drops the
    // prediction after a short guard so the real index drives the strip.
    LaunchedEffect(pendingCarouselIndex, playbackState.currentIndex, queue) {
        val pending = pendingCarouselIndex ?: return@LaunchedEffect
        delay(CarouselReconcileGuardMs)
        if (pendingCarouselIndex == pending && playbackState.currentIndex != pending) {
            pendingCarouselIndex = null
        }
    }

    /**
     * Predicts the queue index a skip tap lands on, matching
     * PlayerController's exact rules so the strip can start moving before the
     * playback state confirms. The displayed queue is already in native
     * playback order while shuffling, so index ± 1 is the predicted pick
     * there. Null when the outcome can't be resolved in display geometry
     * (the queue edge under shuffle) or nothing moves (repeat-one restart,
     * previous beyond the 3s restart threshold, repeat-off at the end).
     */
    fun predictSkipIndex(direction: Int): Int? {
        if (queue.isEmpty()) return null
        // Chain from the pending prediction when one is in flight (PixelPlayer
        // predicts from pendingCarouselIndex too): a rapid next-next burst
        // targets index+2 in one continuous spring instead of pausing at
        // index+1. A wrong chain is corrected by the reconciliation effect.
        val index = (pendingCarouselIndex ?: playbackState.currentIndex)
            .coerceIn(0, queue.lastIndex)
        return if (direction > 0) {
            when {
                playbackState.repeatMode == RepeatMode.ONE -> index
                index < queue.lastIndex -> index + 1
                playbackState.isShuffled -> null
                playbackState.repeatMode == RepeatMode.ALL -> 0
                else -> null
            }
        } else {
            // skipToPrevious restarts the current song when >3s in.
            if (playbackState.positionMs > 3000L) index
            else when {
                index > 0 -> index - 1
                playbackState.isShuffled -> null
                playbackState.repeatMode == RepeatMode.ALL -> queue.lastIndex
                else -> index
            }
        }
    }
    // Latest composed song, read by the song-change effect below (which must
    // not restart just because the effect's captured `song` went stale).
    val latestSong by rememberUpdatedState(song)

    LaunchedEffect(song.id) {
        // Warm the shared memory cache without blocking; the bitmap swaps in
        // on the strip's next frame.
        song.albumArtUri?.let { uri ->
            MiniMusicImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }

        badgeReady = false
        badgeScale.snapTo(0.8f)
        formatInfo = readAudioFormatInfo(context, song.contentUri)
        if (latestSong.id == song.id) {
            badgeReady = true
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = MiniMusicMotion.fastSpatial()
            )
        }
    }

    LaunchedEffect(playbackState.queue, playbackState.currentIndex) {
        // Preload both travel directions: skip-back art must be decoded before
        // it is needed too, or the carousel hitches exactly when travelling
        // backwards. Same neighbor-prefetch reasoning as PixelPlayer.
        ((playbackState.currentIndex - 2)..(playbackState.currentIndex + 2))
            .filter { it in playbackState.queue.indices && it != playbackState.currentIndex }
            .forEach { preloadAlbumArt(context, playbackState.queue[it]) }
    }

    val landscapeTransportButtonSize = if (isLandscape) 64.dp else TransportButtonSize
    val landscapeTransportCircleSize = if (isLandscape) 64.dp else TransportCircleSize
    val landscapeCapsuleHeight = if (isLandscape) 46.dp else CapsuleSegmentHeight
    val landscapeTimestampTransportGap = if (isLandscape) 2.dp else 0.dp
    val landscapeControlSectionGap = if (isLandscape) 16.dp else ControlSectionGap
    val landscapeFunctionSectionGap = if (isLandscape) 16.dp else FunctionSectionGap

    val artworkBlock: @Composable (Modifier) -> Unit = { modifier ->
        // M3E elevation: a soft drop shadow lifts the artwork off the player
        // canvas (a busy, art-tinted background — exactly the case the M3
        // elevation guidance assigns to visible shadows). Static on the block
        // itself; the film-strip animations inside are untouched.
        Surface(
            modifier = modifier,
            shape = ArtCornerShape,
            color = artColors.primaryContainer,
            tonalElevation = 0.dp,
            shadowElevation = if (artworkShadowEnabled) artworkShadowDp.dp else 0.dp
        ) {
            QueueArtStrip(
                progress = carouselProgress,
                queue = renderedQueue,
                focusedIndex = focusedIndex,
                artColors = artColors,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val metadataAndSeekBlock: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            VerticalMetadataStrip(
                progress = carouselProgress,
                queue = renderedQueue,
                focusedIndex = focusedIndex,
                centeredTitle = centeredTitle,
                artColors = artColors,
                windowHeight = if (isLandscape) 48.dp else 72.dp,
                topPadding = if (isLandscape) 0.dp else ContentSectionGap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 48.dp else 72.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .padding(top = ContentSectionGap)
            ) {
                FlatMusicSlider(
                    value = playbackState.positionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                    valueRange = 0f..playbackState.durationMs.toFloat().coerceAtLeast(1f),
                    onValueChange = { onSeekTo(it.toLong()) },
                    activeColor = artColors.onPrimaryContainer,
                    inactiveColor = artColors.onPrimaryContainer.copy(alpha = 0.2f),
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
                        // A running playhead is the spec's clock case: tabular
                        // figures stop the digits counting up from nudging the
                        // duration off the far edge on every tick.
                        style = MiniMusicType.tabular(MaterialTheme.typography.labelMedium),
                        color = artColors.onPrimaryContainer
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
                                color = artColors.onPrimaryContainer.copy(alpha = 0.1f),
                                modifier = Modifier.graphicsLayer {
                                    scaleX = badgeScale.value
                                    scaleY = badgeScale.value
                                }
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = artColors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        formatDuration(playbackState.durationMs),
                        style = MiniMusicType.tabular(MaterialTheme.typography.labelMedium),
                        color = artColors.onPrimaryContainer
                    )
                }
            }
        }
    }

    val transportBlock: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(landscapeTransportButtonSize)
                .then(
                    if (isLandscape) Modifier
                    else Modifier.padding(top = landscapeControlSectionGap)
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                shape = CircleShape,
                containerColor = artColors.secondaryFixedDim,
                contentColor = artColors.onSecondaryFixed,
                onClick = {
                    if (hapticsEnabled) view.performMiniMusicHaptic()
                    pendingCarouselIndex = predictSkipIndex(-1)
                    onSkipPrevious()
                },
                modifier = Modifier.requiredSize(landscapeTransportCircleSize)
            )
            PlayPauseButton(
                isPlaying = playbackState.isPlaying,
                containerColor = artColors.tertiaryFixedDim,
                contentColor = artColors.onTertiaryFixed,
                onClick = {
                    if (hapticsEnabled) view.performMiniMusicHaptic()
                    onTogglePlayPause()
                },
                modifier = Modifier
                    .weight(1f)
                    .requiredHeight(landscapeTransportButtonSize)
            )
            TransportButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = "Next",
                shape = CircleShape,
                containerColor = artColors.secondaryFixedDim,
                contentColor = artColors.onSecondaryFixed,
                onClick = {
                    if (hapticsEnabled) view.performMiniMusicHaptic()
                    pendingCarouselIndex = predictSkipIndex(1)
                    onSkipNext()
                },
                modifier = Modifier.requiredSize(landscapeTransportCircleSize)
            )
        }
    }

    val functionBlock: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .then(
                    if (isLandscape) Modifier
                    else Modifier.padding(top = landscapeFunctionSectionGap)
                )
        ) {
            // PixelPlayer's capsule track: surfaceContainerLowest at 70% —
            // visible in both light and dark without a neutral band.
            Surface(
                shape = RoundedCornerShape(50),
                color = artColors.surfaceContainerLowest.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(landscapeCapsuleHeight)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp).fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // One distinct vivid hue per active segment (PixelPlayer's
                    // capsule rule): repeat = secondary, shuffle = primary,
                    // lyrics = tertiary.
                    CapsuleSegment(
                        artColors = artColors,
                        activeContainer = artColors.secondaryFixed,
                        activeContent = artColors.onSecondaryFixed,
                        icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        active = playbackState.repeatMode != RepeatMode.OFF,
                        contentDescription = "Repeat",
                        onClick = { if (hapticsEnabled) view.performMiniMusicHaptic(); onCycleRepeat() },
                        isFirst = true,
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleSegment(
                        artColors = artColors,
                        activeContainer = artColors.primaryFixed,
                        activeContent = artColors.onPrimaryFixed,
                        icon = Icons.Filled.Shuffle,
                        active = playbackState.isShuffled,
                        contentDescription = "Shuffle",
                        onClick = { if (hapticsEnabled) view.performMiniMusicHaptic(); onToggleShuffle() },
                        modifier = Modifier.weight(1f)
                    )
                    CapsuleSegment(
                        artColors = artColors,
                        activeContainer = artColors.tertiaryFixed,
                        activeContent = artColors.onTertiaryFixed,
                        icon = Icons.Filled.Subtitles,
                        active = false,
                        contentDescription = "Lyrics",
                        onClick = { if (hapticsEnabled) view.performMiniMusicHaptic(); onOpenLyrics() },
                        isLast = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    val belowArtworkBlock: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier) {
            metadataAndSeekBlock(Modifier.fillMaxWidth())
            transportBlock(Modifier.fillMaxWidth())
            functionBlock(Modifier.fillMaxWidth())
        }
    }

    if (isLandscape) {
        PixelPlayerLandscapeContent(
            modifier = modifier,
            albumCoverSection = { artworkModifier -> artworkBlock(artworkModifier) },
            controlsSection = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        metadataAndSeekBlock(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(landscapeTimestampTransportGap))
                        transportBlock(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(landscapeControlSectionGap))
                        functionBlock(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(landscapeFunctionSectionGap))
                        if (landscapeQueueVisible) {
                            // The bar keeps its 72dp slot in this column in
                            // both states, so the player's layout never moves.
                            // It slides out of that slot and fades while the
                            // sheet rises over the pane.
                            landscapeQueueContent(
                                LandscapeQueueSlot.BAR,
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .height(72.dp)
                            )
                        }
                    }
                    if (landscapeQueueVisible) {
                        landscapeQueueContent(
                            LandscapeQueueSlot.SHEET,
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
        // One critically-damped slow-effects spring drives the whole roles
        // lerp: full-screen color transitions use the slow token (M3E effects
        // rule, no overshoot) so a track change eases instead of stomping.
        progress.animateTo(
            targetValue = 1f,
            animationSpec = MiniMusicMotion.slowEffects()
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
                .background(contentColor.copy(alpha = pressOverlayAlpha))
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
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                // The icon's alpha and its size are different kinds of property,
                // so they keep different specs on purpose — alpha is an effects
                // property and must never overshoot, size is spatial and may —
                // but both now come from the token file and settle on the
                // *fast* role together. They previously ran 130ms/90ms raw
                // tweens against a spring, so the icon finished fading at a
                // different moment from when it stopped growing.
                (androidx.compose.animation.fadeIn(MiniMusicMotion.fastEffects()) +
                    androidx.compose.animation.scaleIn(
                        initialScale = 0.82f,
                        animationSpec = MiniMusicMotion.fastSpatial()
                    )) togetherWith
                    (androidx.compose.animation.fadeOut(MiniMusicMotion.fastEffects()) +
                        androidx.compose.animation.scaleOut(
                            targetScale = 0.92f,
                            animationSpec = MiniMusicMotion.fastSpatial()
                        ))
            },
            label = "playPauseMorph"
        ) { playing ->
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = contentColor, modifier = Modifier.size(28.dp)
                )
                Text(text = if (playing) "Pause" else "Play", style = MaterialTheme.typography.titleMedium,
                    color = contentColor, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(contentColor.copy(alpha = pressOverlayAlpha))
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
    activeContainer: Color,
    activeContent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (active) activeContainer else Color.Transparent,
        animationSpec = MiniMusicMotion.defaultEffects(),
        label = "functionTabBackground"
    )
    val contentColor by animateColorAsState(
        // Inactive icons sit at full onSurface on the translucent track —
        // PixelPlayer's inactive capsule treatment (no neutral grey).
        targetValue = if (active) activeContent else artColors.onSurface,
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
                .background(contentColor.copy(alpha = pressOverlayAlpha))
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,

        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
