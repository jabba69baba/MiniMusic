package com.example.minimusic.data

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
            cleanLyricsText(input)
        )
    }

    @Test
    fun preservesTimestampTagsAndOrdinaryBracketedText() {
        val input = """
            [00:01.00]Intro
            [00:02.50][00:03.50]A lyric line
            [00:04.00]I said [arbitrary words] out loud
        """.trimIndent()

        assertEquals(input, cleanLyricsText(input))
    }

    @Test
    fun metadataOnlyContentIsNotDisplayed() {
        assertNull(cleanLyricsText("[ti:After Dark]\n[ar:Mr.Kitty]\n[offset:0]"))
    }
}
