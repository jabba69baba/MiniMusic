package com.example.minimusic.data

import androidx.media3.common.MimeTypes
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.example.minimusic.data.lyrics.LrcTestData
import com.example.minimusic.data.lyrics.LrcTestData2
import com.example.minimusic.data.lyrics.LrcUtils
import com.example.minimusic.data.lyrics.SemanticLyrics
import com.example.minimusic.data.lyrics.SemanticLyrics.SyncedLyrics
import com.example.minimusic.data.lyrics.SemanticLyrics.UnsyncedLyrics
import com.example.minimusic.data.lyrics.SpeakerEntity
import com.example.minimusic.data.lyrics.bestCandidate
import com.example.minimusic.data.lyrics.toLyricsText
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // ---- Gramophone's LrcUtils parser suite (GPL-3.0) ----

    private fun parse(
        lrcContent: String,
        trim: Boolean? = null,
        multiline: Boolean? = null,
        mustSkip: Boolean? = false
    ): SemanticLyrics? {
        if (trim == null) {
            val a = parse(lrcContent, false, multiline, mustSkip)
            val b = parse(lrcContent, true, multiline, mustSkip)
            assertFalse(
                a is SyncedLyrics != b is SyncedLyrics,
                "trim false and true should result in same type of lyrics"
            )
            if (b is SyncedLyrics) {
                assertEquals(
                    (a as SyncedLyrics).text,
                    b.text,
                    "trim false and true should result in same list for this string"
                )
            } else {
                assertEquals(
                    a?.unsyncedText,
                    b?.unsyncedText,
                    "trim false and true should result in same list for this string"
                )
            }
            return a
        }
        if (multiline == null) {
            val a = parse(lrcContent, trim, false, mustSkip)
            val b = parse(lrcContent, trim, true, mustSkip)
            assertFalse(
                a is SyncedLyrics != b is SyncedLyrics,
                "multiline false and true should result in same type of lyrics (trim=$trim)"
            )
            if (b is SyncedLyrics) {
                assertEquals(
                    (a as SyncedLyrics).text,
                    b.text,
                    "multiline false and true should result in same list for this string (trim=$trim)"
                )
            } else {
                assertEquals(
                    a?.unsyncedText,
                    b?.unsyncedText,
                    "multiline false and true should result in same list for this string (trim=$trim)"
                )
            }
            return a
        }
        val a = LrcUtils.parseLyrics(
            lrcContent,
            MimeTypes.AUDIO_FLAC,
            LrcUtils.LrcParserOptions(trim, multiline, null),
            null
        )
        if (mustSkip != null) {
            if (mustSkip) {
                assertTrue(a is UnsyncedLyrics, "expected skip (trim=$trim multiline=$multiline)")
            } else {
                assertFalse(a is UnsyncedLyrics, "expected no skip (trim=$trim multiline=$multiline)")
            }
        }
        return a
    }

    private fun parseSynced(
        lrcContent: String,
        trim: Boolean? = null,
        multiline: Boolean? = null
    ): List<SemanticLyrics.LyricLine>? {
        return (parse(lrcContent, trim, multiline, mustSkip = false) as SyncedLyrics?)?.text
    }

    @Test
    fun emptyInEmptyOut() {
        val emptyLrc = parse("")
        assertNull(emptyLrc)
    }

    @Test
    fun blankInEmptyOut() {
        val blankLrc = parse("   \t  \n    ")
        assertNull(blankLrc)
    }

    @Test
    fun testShortLrc() {
        val lrc = parseSynced("[11:22.33]hello")
        assertNotNull(lrc)
        assertEquals(1, lrc!!.size)
        assertEquals("hello", lrc[0].text)
        assertEquals(682330uL, lrc[0].start)
    }

    @Test
    fun testTemplateLrc1() {
        val lrc = parseSynced(LrcTestData.AS_IT_WAS)
        assertNotNull(lrc)
        assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
    }

    @Test
    fun testTemplateLrcSyntheticNewlines() {
        val lrcS = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = false)
        assertNotNull(lrcS)
        val lrcM = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = true)
        assertNotNull(lrcM)
        assertNotEquals(lrcS!!, lrcM!!)
        assertEquals(2, lrcS.size)
        assertEquals(2, lrcM.size)
        assertEquals("hello", lrcS[0].text)
        assertEquals("hello\ngood morning", lrcM[0].text)
        assertEquals("how are you?", lrcS[1].text)
        assertEquals("how are you?", lrcM[1].text)
    }

    @Test
    fun testTemplateLrc2() {
        val lrc = parseSynced("[11:22.33]hello\n[33:44.55]good morning")
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals("hello", lrc[0].text)
        assertEquals("good morning", lrc[1].text)
    }

    @Test
    fun testTemplateLrcTrimToggle() {
        val a = parseSynced(LrcTestData.AS_IT_WAS_NO_TRIM, trim = false)
        val b = parseSynced(LrcTestData.AS_IT_WAS_NO_TRIM, trim = true)
        assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_FALSE, a)
        assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_TRUE, b)
    }

    @Test
    fun testTemplateLrcTranslate2Compressed() {
        val lrc = parseSynced(LrcTestData.DREAM_THREAD)
        assertNotNull(lrc)
        assertEquals(LrcTestData.DREAM_THREAD_PARSED, lrc)
    }

    @Test
    fun testTemplateLrcZeroTimestamps() {
        // An all-zero LRC is invalid and gets skipped: what comes back is the
        // untimed text, in order, exactly as Gramophone's suite expects.
        val lrc = parse(
            LrcTestData.AS_IT_WAS.replace(
                "\\[(\\d{2}):(\\d{2})([.:]\\d+)?]".toRegex(),
                "[00:00.00]"
            ), mustSkip = true
        )
        assertNotNull(lrc)
        assertEquals(
            LrcTestData.AS_IT_WAS_PARSED.map { it.text },
            lrc!!.unsyncedText.map { it.first }
        )
    }

    @Test
    fun testSyntheticNewLineMultiLineParser() {
        val lrcS = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = false)
        assertNotNull(lrcS)
        val lrcM = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = true)
        assertNotNull(lrcM)
        assertNotEquals(lrcS!!, lrcM!!)
        assertEquals(2, lrcS.size)
        assertEquals(2, lrcM.size)
        assertEquals("hello", lrcS[0].text)
        assertEquals("hello\ngood morning", lrcM[0].text)
        assertEquals("how are you?", lrcS[1].text)
        assertEquals("how are you?", lrcM[1].text)
    }

    @Test
    fun testSimpleMultiLineParser() {
        val lrcS = parseSynced("[11:22.33]hello\ngood morning\n[33:44.55]how are you?", multiline = false)
        assertNotNull(lrcS)
        val lrcM = parseSynced("[11:22.33]hello\ngood morning\n[33:44.55]how are you?", multiline = true)
        assertNotNull(lrcM)
        assertNotEquals(lrcS!!, lrcM!!)
        assertEquals(2, lrcS.size)
        assertEquals(2, lrcM.size)
        assertEquals("hello", lrcS[0].text)
        assertEquals("hello\ngood morning", lrcM[0].text)
        assertEquals("how are you?", lrcS[1].text)
        assertEquals("how are you?", lrcM[1].text)
    }

    @Test
    fun testLongSyncTimestamp() {
        val lrc = parseSynced("[101:56:78]One two three\n[1234:56:78]Four five six")
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals("One two three", lrc[0].text)
        assertEquals(6116780uL, lrc[0].start)
        assertEquals("Four five six", lrc[1].text)
        assertEquals(74096780uL, lrc[1].start)
    }

    @Test
    fun testOffsetMultiLineParser() {
        val lrc = parseSynced(
            "[offset:+3][00:00.004]hello\ngood morning\n[00:00.005]how are you?",
            multiline = true
        )
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals("hello\ngood morning", lrc[0].text)
        assertEquals(1uL, lrc[0].start)
        assertEquals("how are you?", lrc[1].text)
        assertEquals(2uL, lrc[1].start)
    }

    @Test
    fun testBogusOffsetMultiLineParser() {
        val lrc = parseSynced(
            "[offset:+200][00:00.004]hello\ngood morning\n[00:00.005]how are you?",
            multiline = true
        )
        assertNotNull(lrc)
        assertEquals(2, lrc.size)
        assertEquals("hello\ngood morning", lrc[0].text)
        assertEquals(0uL, lrc[0].start)
        assertEquals("how are you?", lrc[1].text)
        assertEquals(0uL, lrc[1].start)
    }

    @Test
    fun testNegativeOffsetMultiLineParser() {
        val lrc = parseSynced(
            "[offset:-200][00:00.004]hello\ngood morning\n[00:00.005]how are you?",
            multiline = true
        )
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals("hello\ngood morning", lrc[0].text)
        assertEquals(204uL, lrc[0].start)
        assertEquals("how are you?", lrc[1].text)
        assertEquals(205uL, lrc[1].start)
    }

    @Test
    fun testDualOffsetMultiLineParser() {
        val lrc = parseSynced(
            "[offset:-200][00:00.004]hello\ngood morning\n[offset:+3][00:00.005]how are you?",
            multiline = true
        )
        assertNotNull(lrc)
        assertEquals(2, lrc.size)
        assertEquals("how are you?", lrc[0].text)
        assertEquals(2uL, lrc[0].start)
        assertEquals("hello\ngood morning", lrc[1].text)
        assertEquals(204uL, lrc[1].start)
    }

    @Test
    fun testEmptyLyricNoTranslation() {
        val lrc = parseSynced(
            "[00:00.29]It's hard to breathe but that's alright\n[00:04.45]\n[00:04.45]Hush\n[00:14.23]\n[00:16.25]Shh"
        )
        assertNotNull(lrc)
        assertEquals(5, lrc!!.size)

        assertEquals("It's hard to breathe but that's alright", lrc[0].text)
        assertEquals(290uL, lrc[0].start)
        assertFalse(lrc[0].isTranslated)
        assertEquals("", lrc[1].text)
        assertEquals(4450uL, lrc[1].start)
        assertFalse(lrc[1].isTranslated)
        assertEquals("Hush", lrc[2].text)
        assertEquals(4450uL, lrc[2].start)
        assertFalse(lrc[2].isTranslated)
        assertEquals("", lrc[3].text)
        assertEquals(14230uL, lrc[3].start)
        assertFalse(lrc[3].isTranslated)
        assertEquals("Shh", lrc[4].text)
        assertEquals(16250uL, lrc[4].start)
        assertFalse(lrc[4].isTranslated)
    }

    @Test
    fun testOnlyWordSyncPoints() {
        val lrc = parseSynced("<00:00.02>a<00:01.00>l\n<00:03.00>b")
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals("al", lrc[0].text)
        assertEquals(20uL, lrc[0].start)
        assertEquals("b", lrc[1].text)
        assertEquals(3000uL, lrc[1].start)
    }

    @Test
    fun testOneLineOneWord() {
        val lrc = parseSynced("[00:00.02]<00:00.02>a<00:01.00>")
        assertNotNull(lrc)
        assertEquals(1, lrc.size)
        assertEquals("a", lrc[0].text)
        assertNotNull(lrc[0].words)
        assertEquals(1, lrc[0].words!!.size)
        assertEquals(0..<1, lrc[0].words!![0].charRange)
        assertEquals(20uL..<1000uL, lrc[0].words!![0].timeRange)
    }

    @Test
    fun testTemplateLrcRenderBenchmark() {
        val lrc = parseSynced(LrcTestData2.RENDER_BENCHMARK, trim = false)
        assertNotNull(lrc)
        assertEquals(LrcTestData2.RENDER_BENCHMARK_PARSED, lrc)
    }

    @Test
    fun testTemplateLrcTranslationType1() {
        val lrc = parseSynced(LrcTestData.ALL_STAR)
        assertNotNull(lrc)
        // Gramophone's fixture predates the hide-same-translations filter: it
        // still contains the empty translated line at 24.3s, which the current
        // filter (faithfully ported here) drops because it repeats the empty
        // original directly above it. Everything else must match verbatim.
        assertEquals(
            LrcTestData.ALL_STAR_PARSED.filterNot { it.isTranslated && it.text.isEmpty() },
            lrc
        )
    }

    @Test
    fun testTemplateLrcExtendedAppleTrimToggle() {
        val lrc = parseSynced(LrcTestData.AM_I_DREAMING, trim = false)
        val lrc2 = parseSynced(LrcTestData.AM_I_DREAMING, trim = true)
        assertNotNull(lrc)
        assertEquals(LrcTestData.AM_I_DREAMING_PARSED_NO_TRIM, lrc)
        assertEquals(LrcTestData.AM_I_DREAMING_PARSED_TRIM, lrc2)
    }

    @Test
    fun testCompressedWordScaling() {
        val lrc = parseSynced("[00:00.100][00:10.100]hello<00:00.200>world<00:01.00>lol")
        assertNotNull(lrc)
        assertEquals(2, lrc!!.size)
        assertEquals(100uL, lrc[0].start)
        assertEquals(10100uL, lrc[1].start)
        assertEquals(10099uL, lrc[0].end)
        assertEquals(11270uL, lrc[1].end)
        assertNotNull(lrc[0].words)
        assertEquals(3, lrc[0].words!!.size)
        assertEquals(100uL, lrc[0].words!![0].timeRange.first)
        assertEquals(200uL - 1uL, lrc[0].words!![0].timeRange.last)
        assertEquals(200uL, lrc[0].words!![1].timeRange.first)
        assertEquals(1000uL - 1uL, lrc[0].words!![1].timeRange.last)
        assertEquals(1000uL, lrc[0].words!![2].timeRange.first)
        assertEquals(10099uL, lrc[0].words!![2].timeRange.last)
        assertEquals(10100uL, lrc[1].start)
        assertNotNull(lrc[1].words)
        assertEquals(3, lrc[1].words!!.size)
        assertEquals(10100uL, lrc[1].words!![0].timeRange.first)
        assertEquals(10200uL - 1uL, lrc[1].words!![0].timeRange.last)
        assertEquals(10200uL, lrc[1].words!![1].timeRange.first)
        assertEquals(11000uL - 1uL, lrc[1].words!![1].timeRange.last)
        assertEquals(11000uL, lrc[1].words!![2].timeRange.first)
        assertEquals(11270uL, lrc[1].words!![2].timeRange.last)
    }

    @Test
    fun testCompressedWithSpaces() {
        assertEquals(
            parseSynced("[00:01.00][00:20.01][00:99.00]Can we find a way back?"),
            parseSynced("[00:01.00] [00:20.01] [00:99.00]Can we find a way back?")
        )
    }

    @Test
    fun testBidirectionalWordSplitting() {
        parseSynced("[00:13.00] <00:13.00>یکtwo", trim = false) // must not crash
        val lrc = parseSynced("[00:13.00] <00:13.00>یکtwo", trim = true)
        assertNotNull(lrc)
        assertEquals(1, lrc.size)
        assertNotNull(lrc[0].words)
        assertEquals(2, lrc[0].words!!.size)
        assertEquals(0..<2, lrc[0].words!![0].charRange)
        assertEquals(2..<5, lrc[0].words!![1].charRange)
    }

    @Test
    fun testParserSkippedHello() {
        parse("hello", mustSkip = true)
    }

    @Test
    fun testParserSkipped2() {
        parse("2", mustSkip = true)
    }

    @Test
    fun testParserSkippedDoesNotEatNewlines() {
        assertEquals(listOf("Hello" to null, "" to null, "It's me" to null),
            parse("Hello\n\nIt's me", mustSkip = true)!!.unsyncedText)
        assertEquals(listOf("Hello" to null, "" to null, "It's me" to null, "" to null),
            parse("Hello\n\nIt's me\n", mustSkip = true)!!.unsyncedText)
    }

    @Test
    fun testParserTtmlTemplate() {
        val ttml = parseSynced(LrcTestData2.TTML_DEATH_BED)
        assertEquals(LrcTestData2.TTML_DEATH_BED_PARSED, ttml)
    }

    @Test
    fun testParserTtmlTemplate2() {
        val ttml = parseSynced(LrcTestData2.TTML_SATISIFED)
        assertEquals(LrcTestData2.TTML_SATISFIED_PARSED, ttml)
    }

    @Test
    fun tsZeroIsNotTranslated() {
        val lrc = parseSynced("[00:00.00]hello[00:01.00]bye")
        assertNotNull(lrc)
        assertEquals(2, lrc.size)
        assertEquals("hello", lrc[0].text)
        assertEquals("bye", lrc[1].text)
        assert(!lrc[0].isTranslated)
        assert(!lrc[1].isTranslated)
    }

    @Test
    fun voiceInsteadOfVoice1WhenNoVoice2() {
        val lrc = parseSynced("[00:00.00]v1:hello\n[bg:[00:01.00]bye]")
        assertNotNull(lrc)
        assertEquals(2, lrc.size)
        assertEquals("hello", lrc[0].text)
        assertEquals("bye", lrc[1].text)
        assertEquals(SpeakerEntity.Voice, lrc[0].speaker)
        assertEquals(SpeakerEntity.VoiceBackground, lrc[1].speaker)
    }

    @Test
    fun voice1WhenThereIsVoice2() {
        val lrc = parseSynced(
            "[00:00.00]v1:hello\n[bg:[00:01.00]bye]\n[00:02.00]v2:hello\n[bg:[00:03.00]bye]"
        )
        assertNotNull(lrc)
        assertEquals(4, lrc.size)
        assertEquals("hello", lrc[0].text)
        assertEquals("bye", lrc[1].text)
        assertEquals("hello", lrc[2].text)
        assertEquals("bye", lrc[3].text)
        assertEquals(SpeakerEntity.Voice1, lrc[0].speaker)
        assertEquals(SpeakerEntity.Voice1Background, lrc[1].speaker)
        assertEquals(SpeakerEntity.Voice2, lrc[2].speaker)
        assertEquals(SpeakerEntity.Voice2Background, lrc[3].speaker)
    }

    @Test
    fun explicitEndFromWordRecognized() {
        val lrc = parseSynced("[00:00.00][00:10.00]<00:01.00>hello<00:02.00><00:03.00>")
        assertNotNull(lrc)
        assertEquals(2, lrc.size)
        assertEquals("hello", lrc[0].text)
        assertEquals(0uL, lrc[0].start)
        assertNotNull(lrc[0].words)
        assertEquals(1, lrc[0].words!!.size)
        assertEquals(1000uL..1999uL, lrc[0].words!![0].timeRange)
        assertEquals(2999uL, lrc[0].end)
        assertEquals("hello", lrc[1].text)
        assertEquals(10000uL, lrc[1].start)
        assertNotNull(lrc[1].words)
        assertEquals(1, lrc[1].words!!.size)
        assertEquals(11000uL..11999uL, lrc[1].words!![0].timeRange)
        assertEquals(12999uL, lrc[1].end)
    }

    // ---- Embedded metadata path (synthetic Media3 Metadata) ----

    @Test
    fun extractSyLtBinaryFrameIsParsedAsSyncedLyrics() {
        // SYLT line breaks live at the start of the following entry (ID3 spec,
        // as the decoder expects): "Hello" then "\nWorld".
        val metadata = createMetadata(
            BinaryFrame("SYLT", makeSyltBody(listOf("Hello" to 0, "\nWorld" to 5_000), "test"))
        )
        val result = LrcUtils.extractAndParseLyrics(44100, MimeTypes.AUDIO_MPEG, metadata, LrcUtils.LrcParserOptions(true, true, null))
        assertTrue(result.isNotEmpty())
        assertTrue(result[0] is SyncedLyrics)
        assertEquals(listOf("Hello", "World"), (result[0] as SyncedLyrics).text.map { it.text })
    }

    @Test
    fun vorbisCommentLyricsAreParsed() {
        val metadata = createMetadata(VorbisComment("LYRICS", "Line one\nLine two"))
        val result = LrcUtils.extractAndParseLyrics(44100, MimeTypes.AUDIO_FLAC, metadata, LrcUtils.LrcParserOptions(true, true, null))
        assertTrue(result.isNotEmpty())
        // A Vorbis LYRICS comment is plain text: it lands unsynced.
        val unsynced = result[0] as UnsyncedLyrics
        assertEquals(listOf("Line one" to null, "Line two" to null), unsynced.unsyncedText)
    }

    @Test
    fun mpegUsltFrameIsParsed() {
        val metadata = createMetadata(BinaryFrame("USLT", makeUsltBody("Lyric line")))
        val result = LrcUtils.extractAndParseLyrics(44100, MimeTypes.AUDIO_MPEG, metadata, LrcUtils.LrcParserOptions(true, true, null))
        assertTrue(result.isNotEmpty())
        // USLT is plain text: it comes back unsynced, rendered as-is by the screen.
        val unsynced = result[0] as UnsyncedLyrics
        assertEquals(listOf("Lyric line" to null), unsynced.unsyncedText)
    }

    @Test
    fun bestCandidatePrefersWordTimedOverPlain() {
        val metadata = createMetadata(
            BinaryFrame("SYLT", makeSyltBody(listOf("Word-timed" to 100), "test")),
            USLT(BinaryFrame("USLT", makeUsltBody("plain text")))
        )
        val result = LrcUtils.extractAndParseLyrics(44100, MimeTypes.AUDIO_MPEG, metadata, LrcUtils.LrcParserOptions(true, true, null))
        val best = result.bestCandidate()
        assertNotNull(best)
        assertTrue(best is SyncedLyrics)
        assertEquals("Word-timed", (best as SyncedLyrics).text.first().text)
    }

    @Test
    fun rendererEmitsDisplayReadyLrc() {
        val text = SyncedLyrics(
            listOf(
                SemanticLyrics.LyricLine(
                    text = "Hello", start = 0uL, end = 1000uL, endIsImplicit = false,
                    words = null, speaker = null, isTranslated = false
                )
            )
        )
        assertEquals("[00:00.00]Hello", text.toLyricsText())
    }

    @Test
    fun rendererJoinsUnsyncedText() {
        val text = UnsyncedLyrics(listOf("First line" to null, "Second line" to null))
        assertEquals("First line\nSecond line", text.toLyricsText())
    }

    @Test
    fun bestCandidateFallsBackToPlainWhenNoTimed() {
        val metadata = createMetadata(
            USLT(BinaryFrame("USLT", makeUsltBody("plain lyrics")))
        )
        val result = LrcUtils.extractAndParseLyrics(44100, MimeTypes.AUDIO_MPEG, metadata, LrcUtils.LrcParserOptions(true, true, null))
        assertEquals("plain lyrics", result.bestCandidate()?.toLyricsText())
    }

    @Test
    fun parseLrcKeepsTimestampsAndDropsMetadata() {
        val input = "[ti:After Dark]\n[offset:0]\n[00:00.10]Actual lyric"
        val lyrics = LrcUtils.parseLyrics(input, null, LrcUtils.LrcParserOptions(true, true, null), null)
        assertNotNull(lyrics)
        val synced = lyrics as SyncedLyrics
        assertEquals(1, synced.text.size)
        assertEquals("Actual lyric", synced.text[0].text)
        assertEquals(100uL, synced.text[0].start)
    }

    private fun createMetadata(vararg entries: Any): androidx.media3.common.Metadata {
        val list = entries.map { it as androidx.media3.common.Metadata.Entry }.toMutableList()
        return androidx.media3.common.Metadata(list)
    }

    private fun BinaryFrame(id: String, data: ByteArray) =
        androidx.media3.extractor.metadata.id3.BinaryFrame(id, data)

    private fun USLT(frame: androidx.media3.extractor.metadata.id3.BinaryFrame) = frame

    /**
     * ID3v2.4 USLT frame body: [encoding][language(3)][descriptor \0][text].
     * Encoding 3 = UTF-8, whose delimiter is a single zero byte.
     */
    private fun makeUsltBody(text: String): ByteArray {
        val header = byteArrayOf(
            3, // UTF-8 encoding
            'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), // ISO-639-2 language
            0 // empty content descriptor, terminated
        )
        return header + text.toByteArray(Charsets.UTF_8) + byteArrayOf(0)
    }

    /**
     * ID3v2.4 SYLT frame body: [encoding][language(3)][timestamp format]
     * [content type][descriptor \0] then per line [text \0][timestamp (4, BE)].
     * Timestamp format 2 = absolute milliseconds; content type 1 = lyrics.
     */
    private fun makeSyltBody(entries: List<Pair<String, Long>>, descriptor: String): ByteArray {
        val charset = Charsets.UTF_8
        val builder = ByteArrayOutputStream()
        builder.write(3) // UTF-8 encoding
        builder.write("eng".toByteArray(Charsets.ISO_8859_1)) // language
        builder.write(2) // timestamp format: absolute milliseconds
        builder.write(1) // content type: lyrics
        builder.write(descriptor.toByteArray(charset))
        builder.write(0) // descriptor terminator (no length prefix in ID3)
        for ((text, ms) in entries) {
            builder.write(text.toByteArray(charset))
            builder.write(0)
            builder.write(((ms ushr 24) and 0xFF).toInt())
            builder.write(((ms ushr 16) and 0xFF).toInt())
            builder.write(((ms ushr 8) and 0xFF).toInt())
            builder.write((ms and 0xFF).toInt())
        }
        return builder.toByteArray()
    }

    private fun lyricArrayToString(lrc: List<SemanticLyrics.LyricLine>?): String {
        val str = StringBuilder()
        if (lrc == null) {
            str.appendLine("null")
        } else {
            str.appendLine("listOf(")
            for (i in lrc) {
                str.appendLine(
                    "\tLyricLine(start = ${i.start}uL, text = \"\"\"${i.text}\"\"\", words = " +
                        "${i.words?.let { "mutableListOf(" + it.joinToString { w ->
                            "SemanticLyrics.Word(timeRange = ${w.timeRange.first}uL..${w.timeRange.last}uL, charRange = ${w.charRange.first}..${w.charRange.last}, isRtl = ${w.isRtl})"
                        } + ")" } ?: "null"}, speaker = " +
                        "${i.speaker?.name?.let { "SpeakerEntity.$it" } ?: "null"}, end = ${i.end}uL, isTranslated = ${i.isTranslated}, " +
                        "endIsImplicit = ${i.endIsImplicit}),"
                )
            }
            str.appendLine(")")
        }
        return str.toString()
    }
}
