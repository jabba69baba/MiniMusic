package com.example.minimusic.data.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-created playlist. */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** MediaStore DATE_ADDED of creation, epoch seconds — for stable ordering. */
    val createdAtSeconds: Long
)

/**
 * One song inside a playlist. Songs are stored by their MediaStore id so the
 * library's deletion of a file naturally drops the row (resolved songs are
 * joined against the live library at read time). [position] preserves the
 * user's manual ordering.
 */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)
