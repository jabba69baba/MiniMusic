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

    @Test
    fun parsesUsltFromTagWithUnsynchronisation() {
        // A tag-level unsync flag stores the payload with 0x00 inserted after
        // every 0xFF. Without decoding that first, the frame walk lands
        // mid-header and the USLT frame behind it is never found.
        val frames = v23Frame("TIT2", byteArrayOf(0x00, 0xFF.toByte(), 0x41)) +
            v23Frame("USLT", usltBody("unsync survivor"))
        val tag = id3TagWith(flags = 0x80, storedPayload = applyUnsync(frames))

        assertEquals("unsync survivor", reader().parseId3Lyrics(ByteArrayInputStream(tag)))
    }

    @Test
    fun parsesTagWithExtendedHeader() {
        // v2.3 extended header: size field excludes itself (6 bytes of flags +
        // padding-size follow). Left unread, the first frame header lands
        // mid-field and no lyric frame is found.
        val ext = ByteArrayOutputStream().apply {
            write(0); write(0); write(0); write(6) // size (excludes itself)
            write(0); write(0)                     // extended flags
            write(0); write(0); write(0); write(0) // size of padding
        }
        val payload = ext.toByteArray() + v23Frame("USLT", usltBody("behind extended header"))
        val tag = id3TagWith(flags = 0x40, storedPayload = payload)

        assertEquals("behind extended header", reader().parseId3Lyrics(ByteArrayInputStream(tag)))
    }

    @Test
    fun parsesV22UltFrame() {
        // ID3v2.2 uses 3-byte frame ids ("ULT"/"SLT") and 6-byte frame
        // headers; the old walker read 10-byte headers and never matched.
        val body = usltBody("v22 lyrics")
        val frame = ByteArrayOutputStream().apply {
            write("ULT".toByteArray(Charsets.US_ASCII))
            write((body.size ushr 16) and 0xFF)
            write((body.size ushr 8) and 0xFF)
            write(body.size and 0xFF)
            write(body)
        }
        val tag = id3TagWith(flags = 0, storedPayload = frame.toByteArray(), version = 2)

        assertEquals("v22 lyrics", reader().parseId3Lyrics(ByteArrayInputStream(tag)))
    }

    @Test
    fun readsLyricsFromFlacVorbisComment() {
        val flac = flacWithComments("LYRICS=[00:01.50]Line one\n[00:03.00]Line two")

        assertEquals(
            "[00:01.50]Line one\n[00:03.00]Line two",
            reader().parseFlacLyrics(ByteArrayInputStream(flac))
        )
    }

    @Test
    fun prefersFlacSyncedLyricsOverPlainLyrics() {
        // Order in the block must not decide: SYNCEDLYRICS wins even when the
        // plain LYRICS comment appears first.
        val flac = flacWithComments(
            "LYRICS=plain text here",
            "SYNCEDLYRICS=[00:00.50]Timed line"
        )

        assertEquals("[00:00.50]Timed line", reader().parseFlacLyrics(ByteArrayInputStream(flac)))
    }

    @Test
    fun readsM4aCopyrightLyricAtom() {
        val text = "[00:02.00]M4A line".toByteArray(Charsets.UTF_8)
        val dataAtom = mp4Box("data", ByteArray(8) + text) // version/flags + locale
        val lyricAtom = mp4BoxRaw(
            byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte()),
            dataAtom
        )
        val meta = mp4Box("meta", ByteArray(4) + mp4Box("ilst", lyricAtom))
        val file = mp4Box("ftyp", "isom".toByteArray(Charsets.US_ASCII) + ByteArray(4)) +
            mp4Box("moov", mp4Box("udta", meta))

        assertEquals("[00:02.00]M4A line", reader().parseMp4Lyrics(ByteArrayInputStream(file)))
    }

    private fun v23Frame(id: String, body: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id.toByteArray(Charsets.US_ASCII))
        val size = body.size
        out.write((size ushr 24) and 0xFF)
        out.write((size ushr 16) and 0xFF)
        out.write((size ushr 8) and 0xFF)
        out.write(size and 0xFF)
        out.write(0) // frame flags, byte 1
        out.write(0) // frame flags, byte 2
        out.write(body)
        return out.toByteArray()
    }

    /** ID3 tag with explicit header flags; [storedPayload] is what the file
     * holds (already unsynchronised if the flag says so). */
    private fun id3TagWith(flags: Int, storedPayload: ByteArray, version: Int = 3): ByteArray {
        val tag = ByteArrayOutputStream()
        tag.write("ID3".toByteArray(Charsets.US_ASCII))
        tag.write(version)
        tag.write(0) // revision
        tag.write(flags)
        val size = storedPayload.size
        tag.write((size ushr 21) and 0x7F)
        tag.write((size ushr 14) and 0x7F)
        tag.write((size ushr 7) and 0x7F)
        tag.write(size and 0x7F)
        tag.write(storedPayload)
        return tag.toByteArray()
    }

    /** The ID3 unsynchronisation encoder: insert 0x00 after every 0xFF. */
    private fun applyUnsync(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (b in data) {
            out.write(b.toInt() and 0xFF)
            if (b == 0xFF.toByte()) out.write(0)
        }
        return out.toByteArray()
    }

    /** A minimal FLAC stream: magic + one last VORBIS_COMMENT block. */
    private fun flacWithComments(vararg comments: String): ByteArray {
        val block = ByteArrayOutputStream()
        val vendor = "minimusic".toByteArray(Charsets.UTF_8)
        writeLe32(block, vendor.size)
        block.write(vendor)
        writeLe32(block, comments.size)
        for (comment in comments) {
            val bytes = comment.toByteArray(Charsets.UTF_8)
            writeLe32(block, bytes.size)
            block.write(bytes)
        }
        val data = block.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray(Charsets.US_ASCII))
        out.write(0x84) // last metadata block | type 4 (VORBIS_COMMENT)
        out.write((data.size ushr 16) and 0xFF)
        out.write((data.size ushr 8) and 0xFF)
        out.write(data.size and 0xFF)
        out.write(data)
        return out.toByteArray()
    }

    private fun writeLe32(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun mp4Box(type: String, content: ByteArray): ByteArray =
        mp4BoxRaw(type.toByteArray(Charsets.US_ASCII), content)

    private fun mp4BoxRaw(type: ByteArray, content: ByteArray): ByteArray {
        val size = 8 + content.size
        val out = ByteArrayOutputStream()
        out.write((size ushr 24) and 0xFF)
        out.write((size ushr 16) and 0xFF)
        out.write((size ushr 8) and 0xFF)
        out.write(size and 0xFF)
        out.write(type)
        out.write(content)
        return out.toByteArray()
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
