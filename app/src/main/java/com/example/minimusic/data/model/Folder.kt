package com.example.minimusic.data.model

/**
 * A storage folder holding audio files, derived from MediaStore's
 * RELATIVE_PATH. Derived, not stored — the same pattern as Album/Artist —
 * so it always matches the scanned library.
 */
data class Folder(
    val path: String,
    val songCount: Int
)
