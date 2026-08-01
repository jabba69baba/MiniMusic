package com.example.minimusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the on-device audio library via MediaStore. Everything here is local:
 * no network calls, no external metadata lookups.
 */
class MusicRepository(private val context: Context) {

    private val artworkUriBase = android.net.Uri.parse("content://media/external/audio/albumart")

    suspend fun loadSongs(minDurationSeconds: Int = 20): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.IS_MUSIC
        )
        // Filter out very short clips (ringtones/notification blips) and non-music audio.
        // The threshold is user-configurable from Settings > Content.
        val minDurationMs = minDurationSeconds * 1000L
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(artworkUriBase, albumId)

                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown title",
                    artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown artist",
                    album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown album",
                    albumId = albumId,
                    durationMs = cursor.getLong(durationCol),
                    trackNumber = cursor.getInt(trackCol).let { if (it > 1000) it % 1000 else it },
                    contentUri = contentUri,
                    albumArtUri = albumArtUri
                )
            }
        }

        songs
    }

    /** Groups the flat song list into albums. Derived, not stored, so it always matches the library. */
    fun deriveAlbums(songs: List<Song>): List<Album> =
        songs.groupBy { it.albumId }
            .map { (albumId, songsInAlbum) ->
                val first = songsInAlbum.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    albumArtUri = first.albumArtUri,
                    songCount = songsInAlbum.size
                )
            }
            .sortedBy { it.title.lowercase() }

    /** Groups the flat song list into artists. Derived, not stored, so it always matches the library. */
    fun deriveArtists(songs: List<Song>): List<Artist> =
        songs.groupBy { it.artist }
            .map { (artist, songsByArtist) ->
                Artist(
                    name = artist,
                    songCount = songsByArtist.size,
                    albumCount = songsByArtist.map { it.albumId }.distinct().size
                )
            }
            .sortedBy { it.name.lowercase() }
}
