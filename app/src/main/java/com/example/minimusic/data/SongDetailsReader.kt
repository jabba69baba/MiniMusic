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
    val path: String?,
    val sizeBytes: Long?
)

private fun readContentSize(context: Context, uri: android.net.Uri): Long? = runCatching {
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
        descriptor.length.takeIf { it >= 0L }
    }
}.getOrNull()

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
        val pathAndSize = context.contentResolver.query(
            song.contentUri,
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            if (cursor.moveToFirst()) {
                val path = if (pathIndex >= 0) cursor.getString(pathIndex) else null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                path to size
            } else {
                null to null
            }
        } ?: (null to null)
        SongDetails(
            albumArtist = albumArtist,
            discNumber = discNumber,
            genre = genre,
            year = year,
            mimeType = mimeType,
            formatInfo = readAudioFormatInfo(context, song.contentUri),
            path = pathAndSize.first?.takeIf { it.isNotBlank() },
            sizeBytes = pathAndSize.second?.takeIf { it >= 0L }
                ?: readContentSize(context, song.contentUri)
        )
    } catch (_: Exception) {
        SongDetails(
            albumArtist = null,
            discNumber = null,
            genre = null,
            year = null,
            mimeType = null,
            formatInfo = readAudioFormatInfo(context, song.contentUri),
            path = null,
            sizeBytes = readContentSize(context, song.contentUri)
        )
    } finally {
        retriever.release()
    }
}
