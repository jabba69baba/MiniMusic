package com.example.minimusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Metadata shown by the song Details screen. All values come from the local file or MediaStore. */
data class SongDetails(
    val albumArtist: String?,
    val discNumber: String?,
    val genre: String?,
    val year: String?,
    val mimeType: String?,
    val formatInfo: AudioFormatInfo?,
    val path: String?
)

suspend fun readSongDetails(context: Context, song: Song): SongDetails = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, song.contentUri)
        val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?.takeIf { it.isNotBlank() }
        val discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            ?.takeIf { it.isNotBlank() }
        val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            ?.takeIf { it.isNotBlank() }
        val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            ?.takeIf { it.isNotBlank() }
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?.takeIf { it.isNotBlank() }
        val path = context.contentResolver.query(
            song.contentUri,
            arrayOf(MediaStore.MediaColumns.DATA),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        SongDetails(
            albumArtist = albumArtist,
            discNumber = discNumber,
            genre = genre,
            year = year,
            mimeType = mimeType,
            formatInfo = readAudioFormatInfo(context, song.contentUri),
            path = path?.takeIf { it.isNotBlank() }
        )
    } catch (_: Exception) {
        SongDetails(
            albumArtist = null,
            discNumber = null,
            genre = null,
            year = null,
            mimeType = null,
            formatInfo = readAudioFormatInfo(context, song.contentUri),
            path = null
        )
    } finally {
        retriever.release()
    }
}
