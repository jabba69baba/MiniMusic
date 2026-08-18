package com.example.minimusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsets.Companion.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.viewmodel.LyricsState

private data class DisplayLyricLine(
    val text: String,
    val startMs: Long? = null
)

private val LrcTimestampRegex = Regex(
    "^\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?](.*)$"
)

private fun parseDisplayLyrics(text: String): List<DisplayLyricLine> {
    return text.lines()
        .filter { it.isNotBlank() }
        .flatMap { rawLine ->
            val match = LrcTimestampRegex.matchEntire(rawLine.trim())
            if (match == null) {
                listOf(DisplayLyricLine(rawLine.trim()))
            } else {
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fractionText = match.groupValues[3]
                val fractionMs = when (fractionText.length) {
                    1 -> fractionText.toLong() * 100L
                    2 -> fractionText.toLong() * 10L
                    3 -> fractionText.toLong()
                    else -> 0L
                }
                listOf(
                    DisplayLyricLine(
                        text = match.groupValues[4].trim().ifBlank { "…" },
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
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val lines = remember(lyricsState) {
        if (lyricsState is LyricsState.Found) parseDisplayLyrics(lyricsState.text) else emptyList()
    }
    val listState = rememberLazyListState()
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

    LaunchedEffect(activeIndex, hasTimedLines) {
        if (hasTimedLines && activeIndex >= 0) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            if (targetIndex != listState.firstVisibleItemIndex) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val inactiveColor = colorScheme.onBackground.copy(alpha = 0.38f)
    val activeColor = colorScheme.primary

    when (lyricsState) {
        LyricsState.Idle, LyricsState.Loading -> CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
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
                .windowInsetsPadding(systemBars)
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
                        .windowInsetsPadding(systemBars)
                        .padding(horizontal = 40.dp, vertical = 48.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background)
                        .windowInsetsPadding(systemBars),
                    contentPadding = PaddingValues(
                        start = 40.dp,
                        end = 32.dp,
                        top = 72.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { index, line -> "${line.startMs ?: -1L}-$index" }
                    ) { index, line ->
                        val isActive = index == activeIndex
                        val color by animateColorAsState(
                            targetValue = if (isActive) activeColor else inactiveColor,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "lyricsLineColor"
                        )
                        val bounce by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 380f
                            ),
                            label = "lyricsLineBounce"
                        )
                        Text(
                            text = line.text,
                            color = color,
                            style = if (isActive) {
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 36.sp
                                )
                            } else {
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 36.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val scale = 1f + (0.025f * bounce)
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = -1.5f * bounce
                                }
                        )
                    }
                }
            }
        }
    }
}
