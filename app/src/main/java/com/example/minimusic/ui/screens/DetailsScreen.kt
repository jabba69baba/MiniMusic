package com.example.minimusic.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.SongDetails
import com.example.minimusic.data.readSongDetails
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.MiniMusicImageLoader
import com.example.minimusic.ui.theme.MiniMusicMotion
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The one gap the dialog uses between its three zones — title, identity block,
 * info cards. Keeping it single-sourced is what removes the dead space that
 * used to sit between the artwork and the first card.
 */
private val DetailSectionGap = 20.dp

/** Gap between the info cards themselves. */
private val DetailCardGap = 8.dp

@Composable
fun DetailsScreen(
    song: Song,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // The dialog follows the app's Monet scheme rather than the playing song's
    // art colors: it is a document about the file, not part of the player
    // canvas (see the art-palette scoping note in Theme.kt).
    val scheme = MaterialTheme.colorScheme
    var details by remember(song.id) { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(song.id) {
        details = readSongDetails(context, song)
    }

    // One animation owns both directions: same duration, same easing, same
    // scale pair, so opening and closing are the same motion played forwards
    // and backwards. Closing runs the exit first and only then pops the route,
    // so the route change can never cut the surface off mid-flight.
    val revealed = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }
    val revealSpec = tween<Float>(
        durationMillis = MiniMusicMotion.dialogDurationMillis,
        easing = MiniMusicMotion.dialogEasing
    )
    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            revealed.animateTo(0f, revealSpec)
            onBack()
        }
    }
    LaunchedEffect(Unit) {
        revealed.animateTo(1f, revealSpec)
    }

    Dialog(onDismissRequest = { dismiss() }) {
        // The platform dialog window runs its own fade, which only ever plays
        // on the way in. Switching it off leaves this surface as the single
        // owner of both directions.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            dialogWindow?.setWindowAnimations(0)
            onDispose { }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.82f)
                .graphicsLayer {
                    val progress = revealed.value
                    alpha = progress
                    val scale = 0.92f + 0.08f * progress
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 20.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface
                )

                Spacer(Modifier.height(DetailSectionGap))

                DetailIdentity(song = song, scheme = scheme)

                Spacer(Modifier.height(DetailSectionGap))

                val loaded = details
                if (loaded == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = scheme.primary)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(DetailCardGap)) {
                        DetailCard(Icons.Filled.Person, "Artist", song.artist, scheme)
                        DetailCard(Icons.Filled.Album, "Album", song.album, scheme)
                        DetailCard(
                            Icons.Filled.Badge,
                            "Album artist",
                            loaded.albumArtist ?: song.artist,
                            scheme
                        )
                        DetailCard(Icons.Filled.Timer, "Duration", formatDuration(song.durationMs), scheme)
                        DetailCard(Icons.Filled.GraphicEq, "Genre", loaded.genre ?: "Unknown", scheme)
                        DetailCard(Icons.Filled.Info, "Year", loaded.year ?: "Unknown", scheme)

                        val format = loaded.formatInfo
                        val audioInfo = buildList {
                            format?.sampleRateHz?.let {
                                add("${String.format(Locale.US, "%.1f", it / 1000f)} kHz")
                            }
                            format?.bitrateKbps?.let { add("$it kbps") }
                            format?.mimeLabel?.let { add(it) }
                        }.joinToString(" • ").ifBlank { "Unknown" }
                        DetailCard(Icons.Filled.AudioFile, "Quality", audioInfo, scheme)
                        DetailCard(Icons.Filled.SdCard, "Size", formatFileSize(loaded.sizeBytes), scheme)
                        DetailCard(
                            Icons.Filled.Storage,
                            "Path",
                            loaded.path ?: song.contentUri.toString(),
                            scheme
                        )
                    }
                }
            }
        }
    }
}

/**
 * The dialog's identity block: artwork at a third of the available width, the
 * remaining two thirds carrying the song name above its artist. Both lines
 * marquee when they don't fit, so a long name is read completely instead of
 * being truncated to a different font size.
 */
@Composable
private fun DetailIdentity(
    song: Song,
    scheme: ColorScheme
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val artSize = maxWidth / 3f
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val artRequest = song.albumArtUri?.let { uri ->
                    remember(uri) {
                        ImageRequest.Builder(context)
                            .data(uri)
                            // No bitmap fade: the tile swaps the instant it
                            // decodes. On decode failure the note icon below
                            // takes over, never an empty tile.
                            .crossfade(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                }
                var artFailed by remember(song.albumArtUri) { mutableStateOf(false) }
                if (artRequest == null || artFailed) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    AsyncImage(
                        model = artRequest,
                        imageLoader = MiniMusicImageLoader.get(context),
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Error) artFailed = true
                        }
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 900,
                            initialDelayMillis = 700,
                            velocity = 19.dp
                        )
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 900,
                            initialDelayMillis = 700,
                            velocity = 19.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    scheme: ColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun formatFileSize(sizeBytes: Long?): String {
    val bytes = sizeBytes ?: return "Unknown"
    if (bytes < 0L) return "Unknown"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (value >= 100.0 || unitIndex == 0) {
        "%.0f %s".format(Locale.US, value, units[unitIndex])
    } else {
        "%.1f %s".format(Locale.US, value, units[unitIndex])
    }
}
