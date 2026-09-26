package com.example.minimusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

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
private val WordTimingLinePrefixRegex = Regex("^\\[\\s*\\d+\\s*,\\s*\\d+\\s*\\]")
private val InstrumentalPlaceholderRegex = Regex(
    "^(?:纯音乐[,，]?请欣赏|純音樂[,，]?請欣賞)$"
)

/**
 * Reads lyrics straight out of a song's own embedded ID3v2 tag (the "USLT" frame —
 * standard unsynchronized lyrics), so nothing is ever fetched from the network.
 *
 * ID3v2 USLT is parsed directly for MP3-family files. For containers such as FLAC
 * and M4A, the platform metadata retriever is used as a lightweight fallback for
 * embedded lyric fields. Network lyrics and sidecar files remain intentionally out
 * of scope for the offline player.
 */
class LyricsReader(private val context: Context) {

    suspend fun readLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        runCatching {
            // Sidecar .lrc wins: most taggers and downloaders ship synced lyrics
            // as "Title.lrc" next to the audio file, and players the user compares
            // against (Poweramp, Musicolet, Metrolist) read exactly this. Embedded
            // tags remain the in-file fallback.
            readSidecarLrc(song)
                ?: context.contentResolver.openInputStream(song.contentUri)?.use { input ->
                    parseId3Lyrics(input)
                }
                ?: readContainerLyrics(song)
        }.getOrNull()
    }

    /**
     * Reads "<audio basename>.lrc" from the same directory as the audio file.
     * The audio path comes from MediaStore's DATA column (absolute path). If the
     * path can't be resolved or no sidecar exists, returns null silently.
     */
    private fun readSidecarLrc(song: Song): String? {
        val audioPath = runCatching {
            context.contentResolver.query(
                song.contentUri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return null
        val audioFile = File(audioPath)
        val candidates = listOf(
            File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc"),
            File(audioFile.parentFile, audioFile.nameWithoutExtension + ".LRC")
        )
        for (candidate in candidates) {
            if (!candidate.isFile) continue
            val text = runCatching { candidate.readText() }.getOrNull() ?: continue
            cleanLyricsText(text)?.let { return it }
        }
        return null
    }

    /**
     * MediaMetadataRetriever exposes common container-level lyric metadata for
     * formats such as FLAC and M4A, where lyrics are not stored in an ID3 USLT
     * frame. This keeps the reader offline and avoids adding a large tag library.
     */
    private fun readContainerLyrics(song: Song): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.contentUri)
            // The lyrics metadata key is not exposed consistently across Android
            // SDK stubs. Resolve it reflectively so older compile SDKs still build.
            val lyricsKey = runCatching {
                MediaMetadataRetriever::class.java
                    .getField("METADATA_KEY_LYRICS")
                    .getInt(null)
            }.getOrNull()
            lyricsKey?.let { retriever.extractMetadata(it) }
                ?.let(::cleanLyricsText)
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun parseId3Lyrics(input: InputStream): String? {
        val header = ByteArray(10)
        if (readFully(input, header) < 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return null
        }
        val majorVersion = header[3].toInt() and 0xFF
        var remaining = synchsafeToInt(header[6], header[7], header[8], header[9])

        var usltResult: String? = null

        while (remaining > 10) {
            val frameHeader = ByteArray(10)
            val read = readFully(input, frameHeader)
            if (read < 10) break
            remaining -= read

            if (frameHeader[0] == 0.toByte()) break

            val frameId = String(frameHeader, 0, 4, Charsets.US_ASCII)
            val frameSize = if (majorVersion >= 4) {
                synchsafeToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
            } else {
                bigEndianToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
            }
            if (frameSize <= 0) continue
            remaining -= frameSize

            if (frameId == "USLT") {
                val body = ByteArray(frameSize)
                readFully(input, body)
                if (usltResult == null) usltResult = decodeUslt(body)
                // Keep scanning: a SYLT frame later in the file is preferred
                // (synced lyrics beat plain text).
            } else if (frameId == "SYLT") {
                // Synchronized lyrics: decode to LRC-style "[mm:ss.xx] line"
                // text, which the display layer already times.
                val body = ByteArray(frameSize)
                readFully(input, body)
                // Prefer SYLT when it decodes to usable timed lines; otherwise
                // keep scanning so a plain USLT can still be found.
                decodeSylt(body)?.let { return it }
            } else {
                skipFully(input, frameSize.toLong())
            }
        }
        return usltResult
    }

    /**
     * ID3v2 SYLT → LRC-style text. Layout: [encoding:1][language:3][timestamp
     * format:1][content type:1][descriptor, null-terminated][sync entries].
     * Each entry: null-terminated text + 4-byte big-endian timestamp in ms
     * (format 2) or ticks (format 1 — rare; skipped to milliseconds is not
     * possible without the MPEG frame rate, so format 1 entries are dropped).
     */
    private fun decodeSylt(body: ByteArray): String? {
        if (body.size < 7) return null
        val charset = when (body[0].toInt() and 0xFF) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val nullWidth = if (charset == Charsets.UTF_16 || charset == Charsets.UTF_16BE) 2 else 1
        val timestampIsMs = body[6].toInt() == 2
        var pos = indexAfterNullTerminator(body, 7, nullWidth)

        val entries = mutableListOf<Pair<Long, String>>()
        while (pos + 4 + nullWidth <= body.size) {
            val textEnd = indexAfterNullTerminator(body, pos, nullWidth)
            if (textEnd < 0 || textEnd + 4 > body.size) break
            val text = String(body, pos, textEnd - nullWidth - pos, charset).trim()
            val stamp = ((body[textEnd].toInt() and 0xFF) shl 24) or
                ((body[textEnd + 1].toInt() and 0xFF) shl 16) or
                ((body[textEnd + 2].toInt() and 0xFF) shl 8) or
                (body[textEnd + 3].toInt() and 0xFF)
            pos = textEnd + 4
            if (text.isNotBlank() && timestampIsMs) entries += stamp.toLong() to text
        }
        if (entries.isEmpty()) return null
        entries.sortBy { it.first }
        val lrc = entries.joinToString("\n") { (ms, text) ->
            val m = ms / 60_000
            val s = (ms % 60_000) / 1000
            val f = (ms % 1000) / 10
            String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s", m, s, f, text)
        }
        // Timestamps must survive cleaning: strip only metadata lines.
        return cleanLyricsText(lrc)
    }

    private fun decodeUslt(body: ByteArray): String? {
        if (body.isEmpty()) return null
        val charset = when (body[0].toInt() and 0xFF) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val nullWidth = if (charset == Charsets.UTF_16 || charset == Charsets.UTF_16BE) 2 else 1

        // Layout: [encoding:1][language:3][content descriptor, null-terminated][lyrics text]
        var pos = 1 + 3
        pos = indexAfterNullTerminator(body, pos, nullWidth)
        if (pos >= body.size) return null

        val text = String(body, pos, body.size - pos, charset)
        return cleanLyricsText(text)
    }

    /**
     * Removes provider metadata and nonstandard word-timing markup without removing
     * genuine Unicode combining marks, including Zalgo-style text.
     */
    internal fun cleanLyricsText(text: String): String? = cleanLyricsTextTopLevel(text)

    private fun indexAfterNullTerminator(body: ByteArray, start: Int, nullWidth: Int): Int {
        var i = start
        while (i + nullWidth <= body.size) {
            val isNull = if (nullWidth == 1) {
                body[i] == 0.toByte()
            } else {
                body[i] == 0.toByte() && body[i + 1] == 0.toByte()
            }
            if (isNull) return i + nullWidth
            i += nullWidth
        }
        return body.size
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val n = input.read(buffer, offset, buffer.size - offset)
            if (n < 0) break
            offset += n
        }
        return offset
    }

    private fun skipFully(input: InputStream, byteCount: Long) {
        var remaining = byteCount
        val buffer = ByteArray(8192)
        while (remaining > 0) {
            val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (n < 0) break
            remaining -= n
        }
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0x7F) shl 21) or ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or (b3.toInt() and 0x7F)

    private fun bigEndianToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int =
        ((b0.toInt() and 0xFF) shl 24) or ((b1.toInt() and 0xFF) shl 16) or
            ((b2.toInt() and 0xFF) shl 8) or (b3.toInt() and 0xFF)
}

/**
 * Top-level implementation of [LyricsReader.cleanLyricsText]. It lives outside the
 * class because it depends only on the file-level regexes and text repair — no
 * Android types — which lets the plain JVM unit test call it directly without
 * needing an Android context or a Robolectric-style runner.
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
