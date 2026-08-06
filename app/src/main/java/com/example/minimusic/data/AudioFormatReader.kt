package com.example.minimusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sample rate, bitrate, and container/codec label for a track, when readable from the file. */
data class AudioFormatInfo(
    val sampleRateHz: Int?,
    val bitrateKbps: Int?,
    val mimeLabel: String?
) {
    /** e.g. "44.1 kHz • 320 kbps • MP3" — omits any part that couldn't be read, returns null if nothing was. */
    fun toBadgeText(): String? {
        val parts = buildList {
            sampleRateHz?.let { add("${"%.1f".format(it / 1000f)} kHz") }
            bitrateKbps?.let { add("$it kbps") }
            mimeLabel?.let { add(it) }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }
}

/**
 * Reads format metadata directly from the audio file via [MediaMetadataRetriever].
 * This is separate from MediaStore's own columns, which don't reliably expose sample
 * rate for every format — retrieving it from the file itself is more consistent.
 * Runs on [Dispatchers.IO]; safe to call from a composable's LaunchedEffect/coroutine.
 */
suspend fun readAudioFormatInfo(context: Context, contentUri: Uri): AudioFormatInfo? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, contentUri)

        val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()

        val bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
        val bitrateKbps = bitrateBps?.let { it / 1000 }

        val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val mimeLabel = mime?.substringAfterLast('/')?.uppercase()?.takeIf { it.isNotBlank() }

        if (sampleRate == null && bitrateKbps == null && mimeLabel == null) {
            null
        } else {
            AudioFormatInfo(sampleRate, bitrateKbps, mimeLabel)
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}
