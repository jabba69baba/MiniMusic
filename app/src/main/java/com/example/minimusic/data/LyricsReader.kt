package com.example.minimusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.minimusic.data.lyrics.EmbeddedMetadataReader
import com.example.minimusic.data.lyrics.LrcUtils
import com.example.minimusic.data.lyrics.bestCandidate
import com.example.minimusic.data.lyrics.toLyricsText
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val LrcMetadataTagRegex = Regex(
    "\\[(?:ti|ar|al|by|offset|re|ve|length|la|au|id|tool|title|artist|album|composer|genre|language)\\s*:[^]]*]",
    RegexOption.IGNORE_CASE
)
private val PlainMetadataLineRegex = Regex(
    "^\\s*(?:title|artist|album|album artist|composer|genre|language|lyrics|by|offset|length|copyright|comment|encoded by)\\s*[:=].*$",
    RegexOption.IGNORE_CASE
)
private val LrcCommentLineRegex = Regex(
    "^\\s*\\[(?:comment|meta|metadata)\\s*:.*]\\s*$",
    RegexOption.IGNORE_CASE
)
private val ProviderControlTagRegex = Regex(
    "\\[(?:id|hash|sign|qq|total|offset|language|tool|source)\\s*:[^]]*]",
    RegexOption.IGNORE_CASE
)
private val WordTimingTokenRegex = Regex("<\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*>")
private val WordTimingLinePrefixRegex = Regex("^\\[\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*]")
private val InstrumentalPlaceholderRegex = Regex(
    "^(?:纯音乐[,，]?请欣赏|純音樂[,，]?請欣賞)$"
)

/**
 * Reads lyrics from the song's own files — never the network. The new order mirrors
 * Gramophone's philosophy: first the sidecar "<basename>.ttml/.srt/.lrc" beside the
 * audio (TTML, then SRT, then LRC), then the lyrics embedded in the file itself by
 * Media3's extractor (full ID3v2 SYLT/USLT/USLT, FLAC Vorbis comments, MP4 "©lyr",
 * Ogg comments — all handled by the same readers ExoPlayer uses), then, as a last
 * resort, the platform metadata retriever for formats this reader does not parse.
 *
 * Distinctly from the previous version, embedded lyrics do not go through hand-rolled
 * byte walking: the extractors return the raw frames, and this module's decode + rank
 * semantics (GPL port from Gramophone) do the heavy lifting.
 */
class LyricsReader(private val context: Context) {

    private val parserOptions = LrcUtils.LrcParserOptions(trim = true, multiLine = true, errorText = null)

    suspend fun readLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        // Each stage falls through on its own: one parser failing must not abort the
        // chain that would have succeeded on the next stage.
        runCatching { readSidecarLrc(song) }.getOrNull()
            ?: runCatching { readEmbeddedLyrics(song) }.getOrNull()
            ?: runCatching { readContainerLyrics(song) }.getOrNull()
    }

    /**
     * Reads "<audio basename>.ttml/^.srt/^.lrc" from the same directory as the audio
     * file. The audio path comes from MediaStore's DATA column (absolute path). If the
     * path can't be resolved or no sidecar exists, returns null silently.
     */
    private fun readSidecarLrc(song: Song): String? {
        val audioFile = resolveAudioFile(song) ?: return null
        val lyrics = LrcUtils.loadAndParseLyricsFile(audioFile, null, parserOptions) ?: return null
        return lyrics.toLyricsText()?.let(::cleanLyricsTextTopLevel)
    }

    /**
     * The new embedded path: Media3's extractor emits the tags, this module decodes
     * and ranks them, and the best candidate is rendered into the LRC text the display
     * layer expects.
     */
    private fun readEmbeddedLyrics(song: Song): String? {
        val audioMetadata = EmbeddedMetadataReader.read(context, song.contentUri) ?: return null
        // Ranked candidate list (word-timed first). Null metadata means no lyric tags.
        val best = LrcUtils.extractAndParseLyrics(
            audioMetadata.sampleRate,
            audioMetadata.sampleMimeType,
            audioMetadata.metadata,
            parserOptions
        ).bestCandidate()
        return best?.toLyricsText()?.let(::cleanLyricsTextTopLevel)
    }

    /**
     * MediaMetadataRetriever exposes common container-level lyric metadata for formats
     * such as Ogg, where lyrics are stored in containers this reader does not parse.
     * This keeps the reader offline and avoids adding a large tag library.
     */
    private fun readContainerLyrics(song: Song): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.contentUri)
            val lyricsKey = runCatching {
                MediaMetadataRetriever::class.java
                    .getField("METADATA_KEY_LYRICS")
                    .getInt(null)
            }.getOrNull()
            lyricsKey?.let { retriever.extractMetadata(it) }
                ?.let(::cleanLyricsTextTopLevel)
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * MediaStore's DATA column is our source of truth for file paths. Returns null
     * when the cursor is unavailable or the column is missing, so callers treat that
     * as "no sidecar on this device".
     */
    internal fun resolveAudioFile(song: Song): File? {
        val path = runCatching {
            context.contentResolver.query(
                song.contentUri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return null
        return path?.let { File(it) }
    }
}

/**
 * Top-level implementation of the text hygiene pass: it depends only on the
 * file-level regexes and text repair — no Android types — which lets the plain
 * JVM unit test call it directly without needing an Android context or a
 * Robolectric-style runner.
 */
internal fun cleanLyricsTextTopLevel(text: String): String? {
    val cleaned = text
        .lineSequence()
        .map { it.trim('\u0000', '\uFEFF', '\u2060').trim() }
        .filter { it.isNotBlank() }
        .filterNot { PlainMetadataLineRegex.matches(it) || LrcCommentLineRegex.matches(it) }
        .map { ProviderControlTagRegex.replace(it, "") }
        .map { LrcMetadataTagRegex.replace(it, "") }
        .map { WordTimingLinePrefixRegex.replace(WordTimingTokenRegex.replace(it, ""), "") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { InstrumentalPlaceholderRegex.matches(it) }
        .joinToString("\n")
        .trim()
        .ifBlank { null }
    return cleaned?.let(::repairLikelyMojibake)
}
