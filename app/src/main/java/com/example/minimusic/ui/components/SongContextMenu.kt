package com.example.minimusic.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Song

/**
 * The dropdown shown from a song row's three-dot button. All playback actions
 * are wired to real playback/library functions; there is no playlist system
 * in this app, so "add to playlist" is intentionally not included.
 */
@Composable
fun SongContextMenu(
    song: Song,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlayNow: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShufflePlayFrom: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onOpenDetails: (Song) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Play") },
            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            onClick = { onPlayNow(song); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Play next") },
            leadingIcon = { Icon(Icons.Filled.PlaylistPlay, contentDescription = null) },
            onClick = { onPlayNext(song); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Add to queue") },
            leadingIcon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
            onClick = { onAddToQueue(song); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Shuffle from here") },
            leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
            onClick = { onShufflePlayFrom(song); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Details") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = {
                onOpenDetails(song)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, song.contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, song.title))
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                showDeleteConfirm = true
                onDismiss()
            }
        )
    }


    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${song.title}\"?") },
            text = { Text("This permanently removes the file from your device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(song)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
