package com.example.minimusic.data.model

import android.net.Uri

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    /**
     * Representative artwork: the first album cover found for this artist's
     * songs — the same offline fallback MediaStore's own artist thumbnails
     * use (PixelPlayer/Gramophone pattern). Null when no song has art.
     */
    val artUri: Uri? = null
)
