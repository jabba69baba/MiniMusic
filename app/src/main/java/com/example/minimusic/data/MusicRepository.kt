package com.example.minimusic.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of attempting to delete a song from MediaStore. */
sealed interface DeleteResult {
    /** The file was deleted successfully. */
    data object Deleted : DeleteResult
    /**
     * Android (10+) requires user confirmation via a system dialog before an app
     * can delete media it doesn't own the underlying file for. [intentSender]
     * should be launched with an ActivityResultLauncher; on a successful result,
     * call [MusicRepository.deleteSong] again for the same song to complete it.
     */
    data class NeedsPermission(val intentSender: IntentSender) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

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
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATE_ADDED
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
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(artworkUriBase, albumId)

                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol)
                        ?.let(::repairLikelyMojibake)
                        ?: "Unknown title",
                    artist = cursor.getString(artistCol)
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::repairLikelyMojibake)
                        ?: "Unknown artist",
                    album = cursor.getString(albumCol)
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::repairLikelyMojibake)
                        ?: "Unknown album",
                    albumId = albumId,
                    durationMs = cursor.getLong(durationCol),
                    trackNumber = cursor.getInt(trackCol).let { if (it > 1000) it % 1000 else it },
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    dateAddedSeconds = cursor.getLong(dateAddedCol)
                )
            }
        }

        songs
    }

    /**
     * Deletes [song]'s underlying file via MediaStore. On Android 10+, deleting a
     * file the app doesn't own requires user confirmation: this returns
     * [DeleteResult.NeedsPermission] with an IntentSender the caller must launch
     * via an ActivityResultLauncher, then call this again for the same song once
     * that completes successfully.
     */
    suspend fun deleteSong(song: Song): DeleteResult = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(song.contentUri, null, null)
            if (rows > 0) DeleteResult.Deleted else DeleteResult.Failed("File not found")
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteResult.NeedsPermission(e.userAction.actionIntent.intentSender)
            } else {
                DeleteResult.Failed(e.message ?: "Permission denied")
            }
        } catch (e: Exception) {
            DeleteResult.Failed(e.message ?: "Unknown error")
        }
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
