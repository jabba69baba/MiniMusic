package com.example.minimusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.components.performMiniMusicHaptic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.minimusic.data.PaletteStyle
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.theme.LocalMiniMusicReducedMotion
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.rememberArtColorRoles
import com.example.minimusic.ui.viewmodel.LyricsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DisplayLyricLine(
    val text: String,
    val startMs: Long? = null
)

private val LrcTimestampRegex = Regex(
    "^(\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?])(.*)$"
)

private const val READING_BAND_FRACTION = 0.36f
private const val LYRIC_TARGET_TOLERANCE_PX = 2

private fun parseDisplayLyrics(text: String): List<DisplayLyricLine> {
    return text.lines()
        .filter { it.isNotBlank() }
        .flatMap { rawLine ->
            val match = LrcTimestampRegex.matchEntire(rawLine.trim())
            if (match == null) {
                listOf(DisplayLyricLine(rawLine.trim()))
            } else {
                val minutes = match.groupValues[2].toLong()
                val seconds = match.groupValues[3].toLong()
                val fractionText = match.groupValues[4]
                val fractionMs = when (fractionText.length) {
                    1 -> fractionText.toLong() * 100L
                    2 -> fractionText.toLong() * 10L
                    3 -> fractionText.toLong()
                    else -> 0L
                }
                listOf(
                    DisplayLyricLine(
                        text = match.groupValues[5].trim().ifBlank { "…" },
                        startMs = minutes * 60_000L + seconds * 1_000L + fractionMs
                    )
                )
            }
        }
        .sortedWith(compareBy<DisplayLyricLine> { it.startMs == null }.thenBy { it.startMs ?: Long.MAX_VALUE })
}

@Composable
fun LyricsScreen(
    playbackFlow: StateFlow<PlaybackUiState>,
    lyricsState: LyricsState,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit,
    albumArtPaletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT
) {
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    // Collected here so the position ticker recomposes only this screen.
    val playbackState by playbackFlow.collectAsState()
    var isExiting by remember { mutableStateOf(false) }
    val exitScope = rememberCoroutineScope()

    val lines = remember(lyricsState) {
        if (lyricsState is LyricsState.Found) parseDisplayLyrics(lyricsState.text) else emptyList()
    }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    // Timestamp tags alone do not prove synchronization. Downloaded lyric
    // providers commonly stamp every line with [00:00.00], which would otherwise
    // make the final line look active for the whole song. Require at least two
    // distinct timestamps with actual progression.
    val hasTimedLines = remember(lines) {
        val timestamps = lines.mapNotNull { it.startMs }.distinct()
        timestamps.size >= 2 && timestamps.zipWithNext().any { (a, b) -> b > a }
    }
    val activeIndex by remember(lines, playbackState.positionMs) {
        derivedStateOf {
            if (!hasTimedLines) {
                -1
            } else {
                lines.withIndex()
                    .filter { it.value.startMs != null }
                    .lastOrNull { it.value.startMs!! <= playbackState.positionMs }
                    ?.index ?: -1
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { scrollJob?.cancel() }
    }

    val artColors = rememberArtColorRoles(playbackState.currentSong?.albumArtUri, albumArtPaletteStyle)
    // Inactive lines take the background's own secondary role, dimmed so the
    // active line is the only emphasis on screen.
    val inactiveColor = artColors.onSurfaceVariant.copy(alpha = 0.45f)
    val activeColor = artColors.primary

    // Single motion owner for this card: one vertical offset drives open
    // (rise) and close (fall) — symmetric, directional, same emphasized clock
    // as every other card. The NavHost LYRICS transitions are None and there
    // is no nested AnimatedVisibility: the old triple-drive (route tween +
    // visibility spring + sheet progress) is what made close fall, stick,
    // then slide left, and open pop out of nowhere.
    // A full-height slide is the most intense motion in the app, so it is the
    // one the guide's reduced-motion rule is really about: "use subtle fades
    // instead of intense sliding or scaling animations". With that setting on,
    // the card rises a short distance and fades instead of travelling the whole
    // pane, still on the single driver below so neither direction can disagree
    // with the other.
    val reducedMotion = LocalMiniMusicReducedMotion.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeightPx = with(density) { maxHeight.toPx() }
        val closedOffsetPx = if (reducedMotion) fullHeightPx * 0.08f else fullHeightPx
        val cardOffsetY = remember(closedOffsetPx) { Animatable(closedOffsetPx) }
        LaunchedEffect(closedOffsetPx) {
            cardOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            )
        }
        BackHandler(enabled = !isExiting) {
            isExiting = true
            exitScope.launch {
                // Close mirrors open (same duration, same decelerate curve,
                // reversed direction) so neither feels faster.
                cardOffsetY.animateTo(
                    targetValue = closedOffsetPx,
                    animationSpec = tween(
                        MiniMusicMotion.navTransitionDurationMillis,
                        easing = MiniMusicMotion.navEnterEasing
                    )
                )
                onBack()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = cardOffsetY.value
                    if (reducedMotion && closedOffsetPx > 0f) {
                        alpha = (1f - cardOffsetY.value / closedOffsetPx).coerceIn(0f, 1f)
                    }
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
        when (lyricsState) {
        LyricsState.Idle, LyricsState.Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(artColors.background)
                .windowInsetsPadding(WindowInsets.systemBars)
        )

        // No-lyrics states (including instrumentals) center on screen; synced
        // lyric lines stay left-aligned below.
        LyricsState.NotFound -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(artColors.background)
                .windowInsetsPadding(WindowInsets.systemBars),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No embedded lyrics found",
                color = artColors.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }

        is LyricsState.Found -> {
            if (lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(artColors.background)
                        .windowInsetsPadding(WindowInsets.systemBars),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lyrics could not be displayed",
                        color = artColors.onBackground,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(artColors.background)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars)
                    ) {
                        // The first and last lines can both travel through this band, just like
                        // Gramophone's large top/bottom lyric padding.
                        val readingBand = (maxHeight * READING_BAND_FRACTION)
                            .coerceIn(160.dp, 280.dp)
                        val readingBandPx = with(density) { readingBand.roundToPx() }
                        LaunchedEffect(activeIndex, hasTimedLines, readingBandPx) {
                            scrollJob?.cancel()
                            if (!hasTimedLines || activeIndex < 0) return@LaunchedEffect
                            // Direct manipulation wins: never fight the user's finger.
                            if (listState.isScrollInProgress) return@LaunchedEffect

                            val targetLineIndex = activeIndex + 1 // account for the top spacer
                            val bandCenter = listState.layoutInfo.viewportStartOffset + readingBandPx

                            scrollJob = scrollScope.launch {
                                // First resolve a distant target into the viewport. The second
                                // pass below is deliberately measured after layout so a variable
                                // title height or device font scale cannot leave a one-line drift.
                                if (listState.layoutInfo.visibleItemsInfo.none { it.index == targetLineIndex }) {
                                    listState.animateScrollToItem(targetLineIndex)
                                    withFrameNanos { }
                                }

                                val activeItem = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == targetLineIndex }
                                    ?: return@launch
                                val measuredLineCenter = activeItem.offset + activeItem.size / 2
                                val delta = measuredLineCenter - bandCenter

                                if (abs(delta) > LYRIC_TARGET_TOLERANCE_PX) {
                                    // No-bounce glide: an underdamped spring
                                    // here overshoots past the reading band and
                                    // corrects back — the visible stomp.
                                    listState.animateScrollBy(
                                        value = delta.toFloat(),
                                        animationSpec = MiniMusicMotion.carouselSpatial()
                                    )
                                }

                                // Correct the final sub-pixel/layout-rounding residue without a
                                // visible second animation. This keeps the active line centered
                                // at the same physical pixel on every lyric transition.
                                withFrameNanos { }
                                val settledItem = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == targetLineIndex }
                                if (settledItem != null) {
                                    val settledDelta = settledItem.offset + settledItem.size / 2 - bandCenter
                                    if (abs(settledDelta) > LYRIC_TARGET_TOLERANCE_PX) {
                                        listState.scrollBy(settledDelta.toFloat())
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 40.dp, end = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            item(key = "lyrics-reading-band-top") {
                                Spacer(modifier = Modifier.height(readingBand))
                            }

                            itemsIndexed(
                                items = lines,
                                key = { index, line -> "${line.startMs ?: -1L}-$index" }
                            ) { index, line ->
                                if (!hasTimedLines) {
                                    // Unsynchronized lyrics are a transcript, not a
                                    // timeline: every line stays active-colored and
                                    // manual scrolling remains completely free.
                                    Text(
                                        text = line.text,
                                        color = activeColor,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 22.sp,
                                            letterSpacing = (-0.1).sp,
                                            lineHeight = 30.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp)
                                    )
                                } else {
                                    val isActive = index == activeIndex
                                    // Fisheye emphasis lives ONLY on the active line:
                                    // it grows and brightens while every other
                                    // line stays identical. Scale rides a
                                    // critically-damped effects spring — the
                                    // underdamped spatial overshoot is what
                                    // stomped. No alpha animation anywhere.
                                    val color by animateColorAsState(
                                        targetValue = if (isActive) activeColor else inactiveColor,
                                        animationSpec = MiniMusicMotion.fastEffects(),
                                        label = "lyricsLineColor"
                                    )
                                    val lineScale by animateFloatAsState(
                                        targetValue = if (isActive) 1.06f else 1f,
                                        animationSpec = MiniMusicMotion.defaultEffects(),
                                        label = "lyricsLineScale"
                                    )
                                    Text(
                                        text = line.text,
                                        color = color,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 22.sp,
                                            letterSpacing = (-0.1).sp,
                                            lineHeight = 30.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp)
                                            .clickable(
                                                enabled = line.startMs != null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                line.startMs?.let { startMs ->
                                                    if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                                    onSeekTo(startMs)
                                                }
                                            }
                                            .graphicsLayer {
                                                scaleX = lineScale
                                                scaleY = lineScale
                                            }
                                    )
                                }
                            }

                            item(key = "lyrics-reading-band-bottom") {
                                Spacer(modifier = Modifier.height(readingBand))
                            }
                        }
                    }
                }
            }
        }
        }
        }
    }
}
