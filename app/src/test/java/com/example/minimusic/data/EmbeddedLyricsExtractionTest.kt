package com.example.minimusic.data

import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Metadata.Entry
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.example.minimusic.data.lyrics.EmbeddedMetadataReader
import com.example.minimusic.data.lyrics.LrcUtils
import com.example.minimusic.data.lyrics.SemanticLyrics
import com.example.minimusic.data.lyrics.bestCandidate
import com.example.minimusic.data.lyrics.toLyricsText
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the container -> tags -> lyrics chain with real Media3 extractors on the JVM.
 *
 * The fixture MP3 is synthesized in-code: a valid ID3v2.4 tag (one USLT frame carrying
 * LRC-style synced lyrics) followed by a run of structurally valid MPEG-1 Layer III
 * frames. The extractor never decodes audio, so zero-filled frame bodies are fine — it
 * only needs the frame headers to be legal. This is the path that was invisible to the
 * older test suite, which only covered text parsing.
 */
class EmbeddedLyricsExtractionTest {

    private val parserOptions = LrcUtils.LrcParserOptions(trim = true, multiLine = true, errorText = null)

    /**
     * android.net.Uri is abstract with a package-private constructor, and the
     * mockable Android jar leaves Uri.EMPTY null, so a mockk stub is the only
     * usable Uri here. FileTypes.inferFileTypeFromUri only consults
     * getLastPathSegment(); null sends it to UNKNOWN (the default extractor
     * order).
     */
    private val mockUri: Uri = mockk {
        every { getLastPathSegment() } returns null
    }

    private val fixtureLyrics =
        "[00:12.34]Hello from embedded lyrics\n" +
            "[00:15.50]Second line of the song\n" +
            "[00:18.00]Last line"

    // ---------------------------------------------------------------------
    // Fixture builders
    // ---------------------------------------------------------------------

    /** ID3v2 syncsafe integer: 4 bytes, 7 bits each, most significant first. */
    private fun syncsafe(value: Int): ByteArray = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    /**
     * USLT frame payload as [UsltFrameDecoder] (Gramophone port) expects it:
     * encoding(ISO-8859-1) + 3-byte language + NUL-terminated description + text.
     * The description is empty so the SYLT mis-detection guard (first text byte
     * 0x01/0x02) can never trip.
     */
    private fun usltPayload(text: String): ByteArray {
        val textBytes = text.toByteArray(Charsets.ISO_8859_1)
        return byteArrayOf(0x00) + "eng".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0x00) + textBytes
    }

    private fun id3v24UsltTag(text: String): ByteArray {
        val payload = usltPayload(text)
        val frame = "USLT".toByteArray(Charsets.ISO_8859_1) +
            syncsafe(payload.size) + byteArrayOf(0x00, 0x00) + payload
        return "ID3".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0x04, 0x00, 0x00) +
            syncsafe(frame.size) + frame
    }

    /** MPEG-1 Layer III, 44.1 kHz, 128 kbps, stereo. Frame length = floor(144 * 128000 / 44100) = 417. */
    private fun mpegFrame(): ByteArray {
        val frame = ByteArray(417)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte() // MPEG-1, Layer III, no CRC
        frame[2] = 0x90.toByte() // 128 kbps, 44.1 kHz, no padding
        frame[3] = 0xC8.toByte() // stereo, original, no emphasis
        return frame
    }

    private fun buildMp3WithUslt(text: String): ByteArray =
        id3v24UsltTag(text) + (0 until 6).fold(ByteArray(0)) { acc, _ -> acc + mpegFrame() }

    /**
     * SYLT frame payload: encoding + language + timestamp format (0x02 = absolute
     * milliseconds) + content type (0x01 = lyrics) + NUL-terminated description, then
     * per line: NUL-terminated text followed by a big-endian 32-bit millisecond
     * timestamp. The last line must end exactly at the end of the payload.
     */
    private fun syltPayload(lines: List<Pair<Long, String>>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(0x00) // ISO-8859-1
        out.write("eng".toByteArray(Charsets.ISO_8859_1))
        out.write(0x02) // timestamp format: absolute milliseconds
        out.write(0x01) // content type: lyrics
        out.write(0x00) // empty description
        for ((timestampMs, text) in lines) {
            out.write(text.toByteArray(Charsets.ISO_8859_1))
            out.write(0x00)
            val ts = timestampMs.toInt()
            out.write((ts shr 24) and 0xFF)
            out.write((ts shr 16) and 0xFF)
            out.write((ts shr 8) and 0xFF)
            out.write(ts and 0xFF)
        }
        return out.toByteArray()
    }

    // ---------------------------------------------------------------------
    // Pure-JVM DataSource so the real extractors can run without Android
    // ---------------------------------------------------------------------

    private class ByteArrayDataSource(private val bytes: ByteArray) : DataSource {
        private var position = 0L

        override fun open(dataSpec: DataSpec): Long {
            position = dataSpec.position.coerceIn(0L, bytes.size.toLong())
            // Media3 1.5.1: open() reports the available length, or
            // C.LENGTH_UNSET when unbounded.
            return C.LENGTH_UNSET.toLong()
        }

        override fun close() {
            // Nothing to release.
        }

        override fun addTransferListener(transferListener: TransferListener) {
            // Not needed for an in-memory source.
        }

        override fun getUri(): Uri? = null

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= bytes.size) return C.RESULT_END_OF_INPUT
            val toRead = minOf(length.toLong(), bytes.size - position).toInt()
            bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + toRead)
            position += toRead
            return toRead
        }
    }

    /** Runs the production extraction path (sniff -> read loop -> tag decode) on raw bytes. */
    private fun extractFromBytes(bytes: ByteArray): List<SemanticLyrics> {
        // The extractor only ever reads position/length from the spec; the
        // Uri instance is a JVM-test stand-in that the factory uses only for
        // file-type inference.
        val dataSpec = DataSpec.Builder()
            .setUri(mockUri)
            .setPosition(0)
            .setLength(C.LENGTH_UNSET.toLong())
            .build()
        val metadata = EmbeddedMetadataReader.readFrom(
            ByteArrayDataSource(bytes),
            dataSpec
        ) ?: return emptyList()
        return LrcUtils.extractAndParseLyrics(
            metadata.sampleRate,
            metadata.sampleMimeType,
            metadata.metadata,
            parserOptions
        )
    }

    private fun metadataWith(vararg entries: Entry) = Metadata(*entries)

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    fun mp3WithUsltFrameReadsEndToEnd() {
        val mp3 = buildMp3WithUslt(fixtureLyrics)

        // Sanity: the synthetic file must actually be sniffed as MP3, not fall
        // through to some other extractor.
        val input = androidx.media3.extractor.DefaultExtractorInput(
            ByteArrayDataSource(mp3), 0, C.LENGTH_UNSET.toLong()
        )
        var sniffed: Extractor? = null
        // The no-arg createExtractors() passes Uri.EMPTY, which is null under
        // the mockable Android jar used by plain JVM unit tests and would NPE
        // in FileTypes.inferFileTypeFromUri — hence the explicit mockUri.
        val factoryCandidates = DefaultExtractorsFactory().createExtractors(mockUri, emptyMap())
        for (candidate in factoryCandidates) {
            input.resetPeekPosition()
            val matched = try {
                candidate.sniff(input)
            } catch (_: Exception) {
                false
            }
            if (matched) {
                sniffed = candidate
                break
            }
        }
        assertNotNull(sniffed, "no extractor sniffed the synthetic MP3")

        val candidates = extractFromBytes(mp3)
        val best = candidates.bestCandidate()
        assertNotNull(best, "no lyrics candidate extracted from the USLT frame")
        assertTrue(best is SemanticLyrics.SyncedLyrics, "expected synced lyrics, got ${best.javaClass}")
        assertEquals(
            fixtureLyrics,
            best.toLyricsText(),
            "rendered lyrics do not round-trip through extractor + decoder"
        )
    }

    @Test
    fun oggVorbisLyricsCommentParsesToSyncedLyrics() {
        // What OggExtractor/VorbisReader attach to the OGG format for an
        // LYRICS=... vorbis comment carrying LRC text.
        val metadata = metadataWith(VorbisComment("LYRICS", fixtureLyrics))
        val candidates = LrcUtils.extractAndParseLyrics(
            44100,
            androidx.media3.common.MimeTypes.AUDIO_VORBIS,
            metadata,
            parserOptions
        )
        val best = candidates.bestCandidate()
        assertNotNull(best)
        assertEquals(fixtureLyrics, best.toLyricsText())
    }

    @Test
    fun mp3SyltFrameParsesToSyncedLyrics() {
        val sylt = syltPayload(
            listOf(
                12_340L to "Synced first line",
                15_500L to "Synced second line"
            )
        )
        val metadata = metadataWith(BinaryFrame("SYLT", sylt))
        val candidates = LrcUtils.extractAndParseLyrics(44100, "audio/mpeg", metadata, parserOptions)
        val best = candidates.bestCandidate()
        assertNotNull(best, "SYLT frame produced no candidate")
        assertEquals(
            "[00:12.34]Synced first line\n[00:15.50]Synced second line",
            best.toLyricsText()
        )
    }

    @Test
    fun mp4UsltTextFrameParses() {
        // Media3's MetadataUtil maps the MP4 "©lyr" atom onto an ID3-style
        // TextInformationFrame with id USLT.
        val metadata = metadataWith(TextInformationFrame("USLT", null, listOf(fixtureLyrics)))
        val candidates = LrcUtils.extractAndParseLyrics(
            44100,
            androidx.media3.common.MimeTypes.AUDIO_MPEG,
            metadata,
            parserOptions
        )
        val best = candidates.bestCandidate()
        assertNotNull(best)
        assertEquals(fixtureLyrics, best.toLyricsText())
    }

    @Test
    fun id3v22UltBinaryFrameParses() {
        // ID3v2.2 writes the USLT frame with the 3-letter id ULT; Media3 keeps
        // the short id, so the decoder must accept it.
        val metadata = metadataWith(BinaryFrame("ULT", usltPayload(fixtureLyrics)))
        val candidates = LrcUtils.extractAndParseLyrics(44100, "audio/mpeg", metadata, parserOptions)
        val best = candidates.bestCandidate()
        assertNotNull(best, "v2.2 ULT frame produced no candidate")
        assertEquals(fixtureLyrics, best.toLyricsText())
    }

    @Test
    fun tagsWithoutLyricsYieldNoCandidates() {
        val metadata = metadataWith(
            TextInformationFrame("TIT2", null, listOf("Some Title")),
            TextInformationFrame("TPE1", null, listOf("Some Artist")),
            BinaryFrame("APIC", ByteArray(16))
        )
        val candidates = LrcUtils.extractAndParseLyrics(44100, "audio/mpeg", metadata, parserOptions)
        assertTrue(candidates.isEmpty(), "non-lyric tags must not produce candidates")
        assertNull(candidates.bestCandidate())
    }

    @Test
    fun plainTextLyricsWithoutTimestampsStayUnsynced() {
        // Some editors embed a plain transcript, no [mm:ss] tags: it must come
        // back as unsynced lyrics, not vanish.
        val plain = "First plain line\nSecond plain line"
        val metadata = metadataWith(BinaryFrame("USLT", usltPayload(plain)))
        val candidates = LrcUtils.extractAndParseLyrics(44100, "audio/mpeg", metadata, parserOptions)
        val best = candidates.bestCandidate()
        assertNotNull(best, "plain-text USLT produced no candidate")
        assertTrue(best is SemanticLyrics.UnsyncedLyrics, "expected unsynced lyrics, got ${best.javaClass}")
        assertEquals(plain, best.toLyricsText())
    }
}
