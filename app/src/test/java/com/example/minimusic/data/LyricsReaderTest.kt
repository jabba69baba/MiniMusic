package com.example.minimusic.data

import android.content.Context
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LyricsReaderTest {
    @Test
    fun removesLrcMetadataHeadersFromEmbeddedText() {
        val input = """
            [ti:After Dark]
            [ar:Mr.Kitty]
            [by:SpotifyLAC-Mobile via Paxsenix API (source: Apple Music (cached fallback))]
            [00:12.34]That I need to ask before I'm alone
        """.trimIndent()

        assertEquals(
            "[00:12.34]That I need to ask before I'm alone",
            cleanLyricsTextTopLevel(input)
        )
    }

    @Test
    fun preservesTimestampTagsAndOrdinaryBracketedText() {
        val input = """
            [00:01.00]Intro
            [00:02.50][00:03.50]A lyric line
            [00:04.00]I said [arbitrary words] out loud
        """.trimIndent()

        assertEquals(input, cleanLyricsTextTopLevel(input))
    }

    @Test
    fun metadataOnlyContentIsNotDisplayed() {
        assertNull(cleanLyricsTextTopLevel("[ti:After Dark]\n[ar:Mr.Kitty]\n[offset:0]"))
    }

    @Test
    fun decodesSyltFrameIntoTimedLrcLines() {
        val tag = id3v23Tag(
            "SYLT" to syltBody(
                entries = listOf("Hello there" to 0, "Second line" to 15_000),
                descriptor = "synced"
            )
        )

        assertEquals(
            "[00:00.00]Hello there\n[00:15.00]Second line",
            reader().parseId3Lyrics(ByteArrayInputStream(tag))
        )
    }

    @Test
    fun decodesSyltFrameWithEmptyDescriptor() {
        // Regression: the timestamp format used to be read from byte 6 (the
        // descriptor's first byte), so spec-compliant frames with an empty
        // descriptor decoded to nothing at all.
        val tag = id3v23Tag(
            "SYLT" to syltBody(entries = listOf("Only line" to 12_340), descriptor = "")
        )

        assertEquals(
            "[00:12.34]Only line",
            reader().parseId3Lyrics(ByteArrayInputStream(tag))
        )
    }

    @Test
    fun prefersSyltOverUsltWhenBothFramesExist() {
        val tag = id3v23Tag(
            "USLT" to usltBody("plain unsynced text"),
            "SYLT" to syltBody(entries = listOf("Timed line" to 5_000), descriptor = "")
        )

        assertEquals(
            "[00:05.00]Timed line",
            reader().parseId3Lyrics(ByteArrayInputStream(tag))
        )
    }

    @Test
    fun fallsBackToUsltWhenSyltUsesTickTimestamps() {
        // Timestamp format $01 (MPEG frames) cannot be converted to ms without
        // the frame rate — the SYLT frame is dropped and plain USLT must win.
        val tag = id3v23Tag(
            "SYLT" to syltBody(entries = listOf("Tick line" to 1), descriptor = "", timestampFormat = 1),
            "USLT" to usltBody("plain unsynced text")
        )

        assertEquals("plain unsynced text", reader().parseId3Lyrics(ByteArrayInputStream(tag)))
    }

    private fun reader() = LyricsReader(mockk<Context>(relaxed = true))

    /** Builds an ID3v2.3 tag: 10-byte header + frames (10-byte frame headers,
     * big-endian sizes) + padding. */
    private fun id3v23Tag(vararg frames: Pair<String, ByteArray>): ByteArray {
        val body = ByteArrayOutputStream()
        for ((id, data) in frames) {
            body.write(id.toByteArray(Charsets.US_ASCII))
            val size = data.size
            body.write((size ushr 24) and 0xFF)
            body.write((size ushr 16) and 0xFF)
            body.write((size ushr 8) and 0xFF)
            body.write(size and 0xFF)
            body.write(0) // frame flags, byte 1
            body.write(0) // frame flags, byte 2
            body.write(data)
        }
        val frameData = body.toByteArray()
        val tag = ByteArrayOutputStream()
        tag.write("ID3".toByteArray(Charsets.US_ASCII))
        tag.write(3) // version 2.3
        tag.write(0) // revision
        tag.write(0) // flags: no unsynchronisation, no extended header
        val tagSize = frameData.size
        tag.write((tagSize ushr 21) and 0x7F) // header size is always synchsafe
        tag.write((tagSize ushr 14) and 0x7F)
        tag.write((tagSize ushr 7) and 0x7F)
        tag.write(tagSize and 0x7F)
        tag.write(frameData)
        tag.write(ByteArray(16)) // padding, as real tags have
        return tag.toByteArray()
    }

    /**
     * SYLT frame body per ID3v2.3 §4.10 / ID3v2.4 §4.9: [encoding][language:3]
     * [time stamp format][content type][descriptor, null-terminated][entries].
     * Each entry is a null-terminated text + 4-byte big-endian timestamp.
     */
    private fun syltBody(
        entries: List<Pair<String, Int>>,
        descriptor: String,
        timestampFormat: Int = 2,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(3) // encoding: UTF-8
        out.write("eng".toByteArray(Charsets.US_ASCII))
        out.write(timestampFormat) // $02 = milliseconds, $01 = MPEG frames
        out.write(1) // content type: lyrics
        out.write(descriptor.toByteArray(Charsets.UTF_8))
        out.write(0) // descriptor terminator
        for ((text, ms) in entries) {
            out.write(text.toByteArray(Charsets.UTF_8))
            out.write(0)
            out.write((ms ushr 24) and 0xFF)
            out.write((ms ushr 16) and 0xFF)
            out.write((ms ushr 8) and 0xFF)
            out.write(ms and 0xFF)
        }
        return out.toByteArray()
    }

    /** USLT frame body: [encoding][language:3][descriptor, null-terminated][text]. */
    private fun usltBody(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(3) // encoding: UTF-8
        out.write("eng".toByteArray(Charsets.US_ASCII))
        out.write(0) // empty descriptor, null-terminated
        out.write(text.toByteArray(Charsets.UTF_8))
        return out.toByteArray()
    }
}
