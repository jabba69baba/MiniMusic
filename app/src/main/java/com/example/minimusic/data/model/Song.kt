package com.example.minimusic.data.model

import android.net.Uri

/**
 * A single audio track, as read from the device's MediaStore.
 * [id] is the MediaStore _ID and doubles as a stable key across the app.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    /** MediaStore's DATE_ADDED, in epoch seconds — when this file was added to the device's library. */
    val dateAddedSeconds: Long
)
