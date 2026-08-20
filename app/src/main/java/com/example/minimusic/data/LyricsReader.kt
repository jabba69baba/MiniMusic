package com.example.minimusic.data

import android.content.Context
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private val LrcMetadataTagRegex = Regex(
    "\\[(?:ti|ar|al|by|offset|re|ve|length|la|au|id|tool)\\s*:[^]]*]",
    RegexOption.IGNORE_CASE
)

/**
 * Reads lyrics straight out of a song's own embedded ID3v2 tag (the "USLT" frame —
 * standard unsynchronized lyrics), so nothing is ever fetched from the network.
 *
 * Current scope: ID3v2.3 / v2.4 tags on MP3-family files (the overwhelming majority
 * of locally-tagged lyrics in the wild). ID3v2.2 (3-char frame IDs) and container
 * formats that store lyrics differently (FLAC "LYRICS" comment, M4A "\xa9lyr" atom)
 * aren't read yet — see the suggestions at the end for extending this.
 */
class LyricsReader(private val context: Context) {

    suspend fun readLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(song.contentUri)?.use { input ->
                parseId3Lyrics(input)
            }
        }.getOrNull()
    }

    private fun parseId3Lyrics(input: InputStream): String? {
        val header = ByteArray(10)
        if (readFully(input, header) < 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return null
        }
        val majorVersion = header[3].toInt() and 0xFF
        var remaining = synchsafeToInt(header[6], header[7], header[8], header[9])

        while (remaining > 10) {
            val frameHeader = ByteArray(10)
            val read = readFully(input, frameHeader)
            if (read < 10) break
            remaining -= read

            if (frameHeader[0] == 0.toByte()) break // padding reached, no more frames

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
                return decodeUslt(body)
            } else {
                skipFully(input, frameSize.toLong())
            }
        }
        return null
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
     * Removes LRC metadata headers such as [ti:], [ar:], [by:] and [offset:]
     * without touching ordinary lyric text or timestamp tags like [00:12.34].
     */
    internal fun cleanLyricsText(text: String): String? = text
        .lineSequence()
        .map { line ->
            LrcMetadataTagRegex.replace(line.trim('\u0000', '\uFEFF').trim(), "").trim()
        }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .trim()
        .ifBlank { null }

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
