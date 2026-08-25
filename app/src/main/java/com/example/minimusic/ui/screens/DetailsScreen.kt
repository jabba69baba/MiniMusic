package com.example.minimusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.SongDetails
import com.example.minimusic.data.readSongDetails
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.theme.rememberArtColorRoles
import java.util.Locale

@Composable
fun DetailsScreen(
    song: Song,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val artColors = rememberArtColorRoles(song.albumArtUri)
    var details by remember(song.id) { mutableStateOf<SongDetails?>(null) }

    LaunchedEffect(song.id) {
        details = readSongDetails(context, song)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(artColors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = artColors.onBackground)
            }
            Text(
                text = "Details",
                style = MaterialTheme.typography.headlineSmall,
                color = artColors.onBackground,
                modifier = Modifier.padding(start = 8.dp)
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
                    .background(artColors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri == null) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = artColors.onPrimaryContainer,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = artColors.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = artColors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        DetailsRule(color = artColors.outlineColor())
        Spacer(Modifier.height(16.dp))

        val loaded = details
        if (loaded == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = artColors.primary)
            }
        } else {
            DetailCard(Icons.Filled.Timer, "Duration", formatDuration(song.durationMs), artColors)
            DetailCard(Icons.Filled.GraphicEq, "Genre", loaded.genre ?: "Unknown", artColors)
            DetailCard(Icons.Filled.Album, "Album", song.album, artColors)
            DetailCard(Icons.Filled.Person, "Artist", song.artist, artColors)
            DetailCard(Icons.Filled.Badge, "Album artist", loaded.albumArtist ?: song.artist, artColors)
            DetailCard(Icons.Filled.Info, "Disc number", loaded.discNumber ?: "Unknown", artColors)
            DetailCard(Icons.Filled.Info, "Track number", song.trackNumber.takeIf { it > 0 }?.toString() ?: "Unknown", artColors)
            DetailCard(Icons.Filled.Info, "Year", loaded.year ?: "Unknown", artColors)

            Spacer(Modifier.height(8.dp))
            DetailsRule(color = artColors.outlineColor())
            Spacer(Modifier.height(16.dp))

            val format = loaded.formatInfo
            val audioInfo = buildList {
                format?.sampleRateHz?.let { add("${String.format(Locale.US, "%.1f", it / 1000f)} kHz") }
                format?.bitrateKbps?.let { add("$it kbps") }
                format?.mimeLabel?.let { add(it) }
            }.joinToString(" • ").ifBlank { "Unknown" }
            DetailCard(Icons.Filled.AudioFile, "Song info", audioInfo, artColors)
            DetailCard(Icons.Filled.GraphicEq, "Bitrate", format?.bitrateKbps?.let { "$it kbps" } ?: "Unknown", artColors)
            DetailCard(Icons.Filled.AudioFile, "MIME type", loaded.mimeType ?: "Unknown", artColors)
            DetailCard(Icons.Filled.Storage, "Path", loaded.path ?: song.contentUri.toString(), artColors)
        }
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    artColors: com.example.minimusic.ui.theme.ArtColorRoles
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(18.dp),
        color = artColors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = artColors.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = artColors.onSurface)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = artColors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailsRule(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private fun com.example.minimusic.ui.theme.ArtColorRoles.outlineColor(): Color =
    onSurfaceVariant.copy(alpha = 0.45f)
