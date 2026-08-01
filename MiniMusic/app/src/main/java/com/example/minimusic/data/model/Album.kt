package com.example.minimusic.data.model

import android.net.Uri

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val songCount: Int
)
