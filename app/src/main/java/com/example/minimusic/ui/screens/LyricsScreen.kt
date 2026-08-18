package com.example.minimusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.viewmodel.LyricsState
import kotlinx.coroutines.Job
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
private const val SCROLL_ANIMATION_DURATION_MS = 520
private val GramophoneMotionEasing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f)

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
    playbackState: PlaybackUiState,
    lyricsState: LyricsState,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val lines = remember(lyricsState) {
        if (lyricsState is LyricsState.Found) parseDisplayLyrics(lyricsState.text) else emptyList()
    }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    val hasTimedLines = remember(lines) { lines.any { it.startMs != null } }
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

    val colorScheme = MaterialTheme.colorScheme
    val inactiveColor = colorScheme.onBackground.copy(alpha = 0.42f)
    val activeColor = colorScheme.primary

    when (lyricsState) {
        LyricsState.Idle, LyricsState.Loading -> CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(32.dp),
            color = activeColor
        )

        LyricsState.NotFound -> Text(
            text = "No embedded lyrics found",
            color = colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 40.dp, vertical = 48.dp)
        )

        is LyricsState.Found -> {
            if (lines.isEmpty()) {
                Text(
                    text = "Lyrics could not be displayed",
                    color = colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 40.dp, vertical = 48.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background)
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

                            val targetLineIndex = activeIndex + 1 // account for the top spacer
                            val layoutInfo = listState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val activeItem = visibleItems.firstOrNull { it.index == targetLineIndex }
                            val bandTop = layoutInfo.viewportStartOffset + readingBandPx
                            val bandBottom = bandTop + (activeItem?.size ?: 0)
                            val needsScroll = activeItem == null ||
                                activeItem.offset < bandTop - with(density) { 8.dp.roundToPx() } ||
                                activeItem.offset > bandBottom + with(density) { 8.dp.roundToPx() }

                            if (!needsScroll) return@LaunchedEffect

                            scrollJob = scrollScope.launch {
                                if (activeItem != null) {
                                    // When the line is measurable, animate the exact pixel delta
                                    // into the reading band using Gramophone's easing curve.
                                    val delta = activeItem.offset - bandTop
                                    listState.animateScrollBy(
                                        value = delta.toFloat(),
                                        animationSpec = tween(
                                            durationMillis = SCROLL_ANIMATION_DURATION_MS,
                                            easing = GramophoneMotionEasing
                                        )
                                    )
                                } else {
                                    // For a seek or a large jump, LazyListState resolves the
                                    // destination while remaining cancellable by the next tick.
                                    listState.animateScrollToItem(
                                        index = targetLineIndex,
                                        scrollOffset = -readingBandPx
                                    )
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
                                val isActive = index == activeIndex
                                val color by animateColorAsState(
                                    targetValue = if (isActive) activeColor else inactiveColor,
                                    animationSpec = tween(
                                        durationMillis = SCROLL_ANIMATION_DURATION_MS,
                                        easing = GramophoneMotionEasing
                                    ),
                                    label = "lyricsLineColor"
                                )
                                val emphasis by animateFloatAsState(
                                    targetValue = if (isActive) 1.015f else 1f,
                                    animationSpec = tween(
                                        durationMillis = SCROLL_ANIMATION_DURATION_MS,
                                        easing = GramophoneMotionEasing
                                    ),
                                    label = "lyricsLineEmphasis"
                                )
                                Text(
                                    text = line.text,
                                    color = color,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        letterSpacing = (-0.1).sp,
                                        lineHeight = 38.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp)
                                        .clickable(enabled = line.startMs != null) {
                                            line.startMs?.let(onSeekTo)
                                        }
                                        .graphicsLayer {
                                            scaleX = emphasis
                                            scaleY = emphasis
                                        }
                                )
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
