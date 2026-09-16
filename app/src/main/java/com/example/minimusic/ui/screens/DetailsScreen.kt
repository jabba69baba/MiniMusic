package com.example.minimusic.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.minimusic.data.SongDetails
import com.example.minimusic.data.readSongDetails
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.MiniMusicImageLoader
import java.util.Locale

@Composable
fun DetailsScreen(
    song: Song,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // The details dialog intentionally does NOT tint from album art: it follows
    // the app's default Monet color scheme (user preference).
    val scheme = MaterialTheme.colorScheme
    var details by remember(song.id) { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(song.id) {
        details = readSongDetails(context, song)
    }

    Dialog(onDismissRequest = onBack) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri == null) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    // No bitmap fade: swaps the instant it decodes. On decode
                    // failure falls back to the note icon, never an empty tile.
                    val detailsArtRequest = remember(song.albumArtUri) {
                        ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .crossfade(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                    var detailsArtFailed by remember(song.albumArtUri) { mutableStateOf(false) }
                    if (detailsArtFailed) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        )
                    } else {
                        AsyncImage(
                            model = detailsArtRequest,
                            imageLoader = MiniMusicImageLoader.get(context),
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onState = { state ->
                                if (state is AsyncImagePainter.State.Error) detailsArtFailed = true
                            }
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = when {
                            song.title.length > 34 -> 16.sp
                            song.title.length > 24 -> 18.sp
                            else -> MaterialTheme.typography.headlineSmall.fontSize
                        }
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val loaded = details
        AnimatedContent(
            targetState = loaded != null,
            transitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
            label = "detailsContentTransition"
        ) { hasDetails ->
            if (!hasDetails) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            } else {
                val resolved = loaded ?: return@AnimatedContent
                Column {
                    DetailCard(Icons.Filled.Timer, "Duration", formatDuration(song.durationMs))
                    DetailCard(Icons.Filled.GraphicEq, "Genre", resolved.genre ?: "Unknown")
                    DetailCard(Icons.Filled.Album, "Album", song.album)
                    DetailCard(Icons.Filled.Person, "Artist", song.artist)
                    DetailCard(Icons.Filled.Badge, "Album artist", resolved.albumArtist ?: song.artist)
                    DetailCard(Icons.Filled.Info, "Year", resolved.year ?: "Unknown")

                    val format = resolved.formatInfo
                    val audioInfo = buildList {
                        format?.sampleRateHz?.let { add("${String.format(Locale.US, "%.1f", it / 1000f)} kHz") }
                        format?.bitrateKbps?.let { add("$it kbps") }
                        format?.mimeLabel?.let { add(it) }
                    }.joinToString(" • ").ifBlank { "Unknown" }
                    DetailCard(Icons.Filled.AudioFile, "Song info", audioInfo)
                    DetailCard(Icons.Filled.Storage, "Size", formatFileSize(resolved.sizeBytes))
                    DetailCard(Icons.Filled.Storage, "Path", resolved.path ?: song.contentUri.toString())
                }
            }
        }
            }
        }
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    // Default Monet theme — no album-art tinting in this dialog.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
