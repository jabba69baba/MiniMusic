package com.example.minimusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
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
private val WordTimingLinePrefixRegex = Regex("^\\[\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*]")
private val InstrumentalPlaceholderRegex = Regex(
    "^(?:纯音乐[,，]?请欣赏|純音樂[,，]?請欣賞)$"
)

/**
 * Reads lyrics from sources the song itself points to, so nothing is ever
 * fetched from the network: first a sidecar "<basename>.lrc" next to the audio,
 * then the file's own embedded storage — ID3v2 (SYLT preferred over USLT,
 * hardened for unsynchronised tags, extended headers, and v2.2 ULT/SLT),
 * FLAC Vorbis comments (SYNCEDLYRICS/LYRICS), or the MP4 "©lyr" atom. The
 * platform metadata retriever remains the last-resort fallback. Network lyrics
 * remain intentionally out of scope for the offline player.
 */
class LyricsReader(private val context: Context) {

    suspend fun readLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        // Each stage falls through on its own: one parser throwing on an
        // exotic file must not abort the chain that would have succeeded on
        // the next stage (a single runCatching around everything hid every
        // fallback behind the first exception).
        runCatching { readSidecarLrc(song) }.getOrNull()
            ?: runCatching { parseEmbeddedLyrics(song) }.getOrNull()
            ?: runCatching { readContainerLyrics(song) }.getOrNull()
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
     * Sniffs the file's container magic and dispatches to the matching
     * embedded parser: ID3v2 for MP3 ("ID3"), FLAC ("fLaC"), MP4/M4A (the
     * "ftyp" atom at offset 4). Unknown containers return null so the
     * retriever fallback in [readContainerLyrics] gets its turn.
     */
    private fun parseEmbeddedLyrics(song: Song): String? {
        val raw = context.contentResolver.openInputStream(song.contentUri) ?: return null
        return raw.use { input ->
            val buffered = BufferedInputStream(input, 16 * 1024)
            buffered.mark(16)
            val head = ByteArray(12)
            var filled = 0
            while (filled < head.size) {
                val n = buffered.read(head, filled, head.size - filled)
                if (n < 0) break
                filled += n
            }
            buffered.reset()
            when {
                filled >= 10 && matchesAt(head, 0, "ID3") -> parseId3Lyrics(buffered)
                filled >= 4 && matchesAt(head, 0, "fLaC") -> parseFlacLyrics(buffered)
                filled >= 12 && matchesAt(head, 4, "ftyp") -> parseMp4Lyrics(buffered)
                else -> null
            }
        }
    }

    private fun matchesAt(head: ByteArray, offset: Int, text: String): Boolean {
        if (offset + text.length > head.size) return false
        for (i in text.indices) {
            if (head[offset + i] != text[i].code.toByte()) return false
        }
        return true
    }

    /**
     * FLAC: a "fLaC" magic followed by metadata blocks. The VORBIS_COMMENT
     * block (type 4) carries LYRICS / SYNCEDLYRICS / UNSYNCEDLYRICS comments —
     * the embedded-lyrics storage FLAC libraries actually use, and one the
     * platform retriever does not surface.
     */
    internal fun parseFlacLyrics(input: InputStream): String? {
        val magic = ByteArray(4)
        if (readFully(input, magic) < 4 || String(magic, Charsets.US_ASCII) != "fLaC") return null
        while (true) {
            val head = ByteArray(4)
            if (readFully(input, head) < 4) return null
            val isLast = (head[0].toInt() and 0x80) != 0
            val type = head[0].toInt() and 0x7F
            val length = ((head[1].toInt() and 0xFF) shl 16) or
                ((head[2].toInt() and 0xFF) shl 8) or (head[3].toInt() and 0xFF)
            if (length < 0) return null
            if (type == 4) {
                val data = ByteArray(length)
                if (readFully(input, data) < length) return null
                return pickVorbisLyric(parseVorbisComments(data))
            }
            skipFully(input, length.toLong())
            if (isLast) return null
        }
    }

    /** Parses a Vorbis comment block into (KEY, value) pairs; malformed input yields []. */
    private fun parseVorbisComments(data: ByteArray): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var pos = 0
        fun le32(at: Int): Int {
            if (at < 0 || at + 4 > data.size) return -1
            return (data[at].toInt() and 0xFF) or
                ((data[at + 1].toInt() and 0xFF) shl 8) or
                ((data[at + 2].toInt() and 0xFF) shl 16) or
                ((data[at + 3].toInt() and 0xFF) shl 24)
        }
        val vendorLen = le32(pos).also { if (it < 0) return out }
        pos += 4 + vendorLen
        val count = le32(pos).also { if (it < 0 || it > 10_000) return out }
        pos += 4
        repeat(count) {
            val len = le32(pos).also { if (it < 0) return out }
            pos += 4
            if (len < 0 || pos + len > data.size) return out
            val comment = String(data, pos, len, Charsets.UTF_8)
            pos += len
            val eq = comment.indexOf('=')
            if (eq > 0) out += comment.substring(0, eq).uppercase() to comment.substring(eq + 1)
        }
        return out
    }

    /** Preference: synced lyrics, then plain, then language-suffixed variants. */
    private fun pickVorbisLyric(comments: List<Pair<String, String>>): String? {
        val priority = listOf("SYNCEDLYRICS", "LYRICS", "UNSYNCEDLYRICS")
        val ordered = priority.mapNotNull { key -> comments.firstOrNull { it.first == key } } +
            comments.filter { it.first.startsWith("LYRICS_") }
        for ((_, value) in ordered) {
            cleanLyricsText(value)?.let { return it }
        }
        return null
    }

    /**
     * MP4/M4A: the "©lyr" item inside moov/udta/(meta/)ilst, with the text in
     * that item's "data" child (8-byte atom header + 4-byte version/flags +
     * 4-byte locale, then the payload). Atom sizes are validated against the
     * enclosing box; any anomaly returns null for the retriever to try.
     */
    internal fun parseMp4Lyrics(input: InputStream): String? = scanMp4Atoms(input, Long.MAX_VALUE)

    private fun scanMp4Atoms(input: InputStream, budget: Long): String? {
        var pos = 0L
        while (pos + 8 <= budget) {
            val header = ByteArray(8)
            if (readFully(input, header) < 8) return null
            pos += 8
            var size = bigEndianToInt(header[0], header[1], header[2], header[3]).toLong() and 0xFFFFFFFFL
            val type = String(header, 4, 4, Charsets.ISO_8859_1)
            var headerLength = 8L
            if (size == 1L) {
                val large = ByteArray(8)
                if (readFully(input, large) < 8) return null
                pos += 8
                headerLength = 16L
                size = 0L
                for (b in large) size = (size shl 8) or (b.toLong() and 0xFF)
            } else if (size == 0L) {
                // "Extends to end of enclosing box" — unknown at this level.
                return null
            }
            if (size < headerLength) return null
            val contentLength = size - headerLength
            if (budget != Long.MAX_VALUE && pos + contentLength > budget) return null

            when {
                type == "moov" || type == "udta" || type == "ilst" -> {
                    val found = scanMp4Atoms(input, contentLength)
                    pos += contentLength
                    if (found != null) return found
                }
                type == "meta" -> {
                    // meta carries a 4-byte version/flags prefix before children.
                    if (contentLength < 4) {
                        skipFully(input, contentLength)
                        pos += contentLength
                    } else {
                        skipFully(input, 4)
                        val found = scanMp4Atoms(input, contentLength - 4)
                        pos += contentLength
                        if (found != null) return found
                    }
                }
                type == "\u00A9lyr" -> {
                    val body = ByteArray(contentLength.coerceIn(0L, 1L shl 20).toInt())
                    if (readFully(input, body) < body.size) return null
                    if (body.size < 16) return null
                    val dataSize = bigEndianToInt(body[0], body[1], body[2], body[3]).toLong() and 0xFFFFFFFFL
                    val dataType = String(body, 4, 4, Charsets.ISO_8859_1)
                    if (dataType != "data" || dataSize < 16 || dataSize > body.size) return null
                    return cleanLyricsText(String(body, 16, (dataSize - 16).toInt(), Charsets.UTF_8))
                }
                else -> {
                    skipFully(input, contentLength)
                    pos += contentLength
                }
            }
        }
        // Fewer than a header's worth of bytes left: consume them so a caller
        // resuming after this box lands exactly at its end.
        val remainder = budget - pos
        if (budget != Long.MAX_VALUE && remainder in 1..7L) skipFully(input, remainder)
        return null
    }

    /**
     * MediaMetadataRetriever exposes common container-level lyric metadata for
     * formats such as Ogg, where lyrics are stored in containers this reader
     * does not parse directly. This keeps the reader offline and avoids adding
     * a large tag library.
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

    /**
     * Walks the whole ID3v2 tag in memory, which is what makes the hardening
     * possible: a tag carrying the unsynchronisation flag is decoded once
     * before frame parsing (ID3v2.3's usual storage for it), the extended
     * header is skipped instead of being misread as a frame, ID3v2.2's 6-byte
     * ULT/SLT frames are understood, and ID3v2.4 per-frame unsync /
     * data-length-indicator flags are honoured. SYLT still wins over USLT
     * when both decode.
     */
    internal fun parseId3Lyrics(input: InputStream): String? {
        val header = ByteArray(10)
        if (readFully(input, header) < 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return null
        }
        val majorVersion = header[3].toInt() and 0xFF
        val tagFlags = header[5].toInt() and 0xFF
        val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
        if (tagSize <= 0 || tagSize > 32 * 1024 * 1024) return null
        val payload = ByteArray(tagSize)
        if (readFully(input, payload) < tagSize) return null

        val tagLevelUnsync = (tagFlags and 0x80) != 0
        val data = if (tagLevelUnsync) deUnsync(payload) else payload

        var pos = 0
        if ((tagFlags and 0x40) != 0) {
            // In v2.2 that bit means the whole tag is compressed (unsupported);
            // in v2.3/v2.4 it introduces the extended header — whose size field
            // is big-endian and excludes itself in v2.3, synchsafe and includes
            // itself in v2.4. Left unread, every following frame header lands
            // mid-field and no lyric frame is ever found.
            if (majorVersion <= 2 || data.size < 4) return null
            pos = if (majorVersion >= 4) {
                synchsafeToInt(data[0], data[1], data[2], data[3])
            } else {
                bigEndianToInt(data[0], data[1], data[2], data[3]) + 4
            }
            if (pos <= 0 || pos > data.size) return null
        }

        val v22 = majorVersion <= 2
        val frameHeaderSize = if (v22) 6 else 10
        var usltResult: String? = null

        while (pos + frameHeaderSize <= data.size) {
            val idLength = if (v22) 3 else 4
            val rawId = String(data, pos, idLength, Charsets.US_ASCII)
            if (rawId[0] == '\u0000') break // padding
            if (!rawId.all { it in 'A'..'Z' || it in '0'..'9' }) break // garbage → stop
            val frameId = when {
                !v22 -> rawId
                rawId == "ULT" -> "USLT"
                rawId == "SLT" -> "SYLT"
                else -> rawId
            }

            val sizeAt = pos + idLength
            val frameSize = when {
                v22 -> bigEndian3(data[sizeAt], data[sizeAt + 1], data[sizeAt + 2])
                majorVersion >= 4 ->
                    synchsafeToInt(data[sizeAt], data[sizeAt + 1], data[sizeAt + 2], data[sizeAt + 3])
                else ->
                    bigEndianToInt(data[sizeAt], data[sizeAt + 1], data[sizeAt + 2], data[sizeAt + 3])
            }
            if (frameSize < 0 || pos + frameHeaderSize + frameSize > data.size) break
            val bodyStart = pos + frameHeaderSize
            val bodyEnd = bodyStart + frameSize

            var frameUnsync = false
            var hasDataLengthIndicator = false
            if (!v22) {
                val formatFlags = data[pos + 9].toInt() and 0xFF
                if (majorVersion >= 4) {
                    if (formatFlags and 0x08 != 0 || formatFlags and 0x04 != 0) {
                        // Compressed/encrypted bodies have a shape this reader
                        // doesn't model — skip the frame, keep walking.
                        pos = bodyEnd
                        continue
                    }
                    frameUnsync = formatFlags and 0x02 != 0
                    hasDataLengthIndicator = formatFlags and 0x01 != 0
                } else if (formatFlags and 0x80 != 0) {
                    pos = bodyEnd // v2.3 compressed frame
                    continue
                }
            }

            if (frameId == "USLT" || frameId == "SYLT") {
                var body = data.copyOfRange(bodyStart, bodyEnd)
                if (frameUnsync && !tagLevelUnsync) body = deUnsync(body)
                if (hasDataLengthIndicator && body.size > 4) body = body.copyOfRange(4, body.size)
                when (frameId) {
                    // Keep scanning after USLT: a SYLT frame later in the tag
                    // is preferred (synced lyrics beat plain text).
                    "USLT" -> if (usltResult == null) usltResult = decodeUslt(body)
                    "SYLT" -> decodeSylt(body)?.let { return it }
                }
            }
            pos = bodyEnd
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
        // Spec layout (ID3v2.3 §4.10 / ID3v2.4 §4.9): [0] encoding, [1..3]
        // language, [4] time stamp format ($02 = milliseconds), [5] content
        // type, [6..] content descriptor (null-terminated), then sync entries.
        // Reading the format from [6] would hit the descriptor's first byte and
        // silently drop every entry.
        val timestampIsMs = body[4].toInt() == 2
        var pos = indexAfterNullTerminator(body, 6, nullWidth)

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

    private fun bigEndian3(b0: Byte, b1: Byte, b2: Byte): Int =
        ((b0.toInt() and 0xFF) shl 16) or ((b1.toInt() and 0xFF) shl 8) or (b2.toInt() and 0xFF)

    /**
     * Undoes ID3 unsynchronisation: a 0x00 was inserted after every 0xFF byte
     * when the tag was written, so every such 0x00 goes away on read.
     */
    private fun deUnsync(data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var written = 0
        var i = 0
        while (i < data.size) {
            val b = data[i]
            out[written++] = b
            i += if (b == 0xFF.toByte() && i + 1 < data.size && data[i + 1] == 0.toByte()) 2 else 1
        }
        return out.copyOfRange(0, written)
    }
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
