/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Ported into MiniMusic from Gramophone
// (https://github.com/FoedusProgramme/Gramophone), still under the GPL-3.0 terms
// above. Adaptations: the MediaStore/Context sidecar lookup became direct File
// access (MiniMusic resolves paths through MediaStore's DATA column itself),
// feature flags are inlined, logging is gone, a decode failure now skips the
// offending entry instead of aborting the whole chain, and MiniMusic-specific
// helpers (bestCandidate/toLyricsText) were added for the single lyrics view.
package com.example.minimusic.data.lyrics

import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

object LrcUtils {

    enum class LyricFormat {
        LRC,
        TTML,
        SRT
    }

    data class LrcParserOptions(val trim: Boolean, val multiLine: Boolean, val errorText: String?)

    /** Gramophone's Flags.HIDE_SAME_TRANSLATIONS: drop a translated line that merely
     *  repeats the previous non-translated line. */
    private const val HIDE_SAME_TRANSLATIONS = true

    fun parseLyrics(
        lyrics: String,
        audioMimeType: String?,
        parserOptions: LrcParserOptions,
        format: LyricFormat?
    ): SemanticLyrics? {
        val attempts = listOf(
            {
                if (format == null || format == LyricFormat.TTML)
                    parseTtml(audioMimeType, lyrics)
                else null
            },
            {
                if (format == null || format == LyricFormat.SRT)
                    parseSrt(lyrics, parserOptions.trim)
                else null
            },
            {
                if (format == null || format == LyricFormat.LRC)
                    parseLrc(lyrics, parserOptions.trim, parserOptions.multiLine)
                else null
            }
        )
        for (attempt in attempts) {
            val ret = try {
                val parsed = attempt() ?: continue
                if (parsed is SemanticLyrics.SyncedLyrics && HIDE_SAME_TRANSLATIONS) {
                    parsed.copy(text = parsed.text.filterIndexed { i, line ->
                        !line.isTranslated ||
                            line.text != parsed.text.subList(0, i)
                                .last { !it.isTranslated }.text
                    })
                } else {
                    parsed
                }
            } catch (e: Exception) {
                // MiniMusic has no "failed to parse" text to surface: a parser blowing
                // up means "this format isn't it", so the next attempt gets its turn.
                if (parserOptions.errorText == null) continue
                SemanticLyrics.UnsyncedLyrics(listOf(parserOptions.errorText to null))
            }
            return ret
        }
        return null
    }

    // returns best lyrics first (Gramophone's ranking: unsynced first, then line
    // synced, then word-timed; MiniMusic picks its own favourite via bestCandidate)
    fun extractAndParseLyrics(
        sampleRate: Int,
        audioMimeType: String?,
        metadata: Metadata,
        parserOptions: LrcParserOptions
    ): List<SemanticLyrics> {
        val out = mutableListOf<SemanticLyrics>()
        for (i in 0 until metadata.length()) {
            val meta = metadata.get(i)
            if (meta is BinaryFrame && (meta.id == "SYLT" || meta.id == "SLT")) {
                val syltData = try {
                    UsltFrameDecoder.decodeSylt(sampleRate, ParsableByteArray(meta.data))
                } catch (_: Exception) {
                    // Adaptation: a broken frame skips itself, not the whole file.
                    continue
                }
                if (syltData != null) {
                    if (syltData.contentType == 1 || syltData.contentType == 2) {
                        try {
                            out.add(
                                if (HIDE_SAME_TRANSLATIONS) {
                                    val ret = syltData.toSyncedLyrics(parserOptions.trim)
                                    ret.copy(text = ret.text.filterIndexed { i, line ->
                                        !line.isTranslated ||
                                            line.text != ret.text.subList(0, i)
                                                .last { !it.isTranslated }.text
                                    })
                                } else {
                                    syltData.toSyncedLyrics(parserOptions.trim)
                                }
                            )
                        } catch (_: Exception) {
                            // Same skip-not-abort adaptation as above.
                        }
                    }
                    continue
                }
            }
            val plainTextData =
                if (meta is VorbisComment && meta.key == "LYRICS") // vorbis comments
                    meta.value
                else if (meta is BinaryFrame && (meta.id == "USLT" || meta.id == "ULT"
                            || meta.id == "SLT" /* out-of-spec */
                            || meta.id == "SYLT" /* out-of-spec */)
                ) // ID3
                    UsltFrameDecoder.decode(ParsableByteArray(meta.data))?.text
                else if (meta is TextInformationFrame && meta.id == "USLT") // mp4
                    meta.values.joinToString("\n")
                else null
            if (plainTextData != null) {
                try {
                    parseLyrics(plainTextData, audioMimeType, parserOptions, null)?.let {
                        out.add(it)
                        continue
                    }
                } catch (_: Exception) {
                    // Skip this entry; other entries may still parse.
                }
            }
        }
        out.sortBy {
            if (it !is SemanticLyrics.SyncedLyrics) {
                return@sortBy -10
            }
            val hasWords = it.text.find { it.words != null } != null
            val hasTl = it.text.find { it.isTranslated } != null
            if (hasWords) 10 else 0 + if (hasTl) 1 else 0
        }
        return out
    }

    /**
     * Looks for a sidecar "<basename>.ttml/.srt/.lrc" next to the audio file, in
     * that priority order, and parses the first one that exists. This is the
     * direct-File equivalent of Gramophone's MediaStore-based lookup.
     */
    fun loadAndParseLyricsFile(
        musicFile: File?,
        audioMimeType: String?,
        parserOptions: LrcParserOptions
    ): SemanticLyrics? {
        if (musicFile == null) return null
        val extensions = listOf("ttml", "srt", "lrc")
        val formats = listOf(LyricFormat.TTML, LyricFormat.SRT, LyricFormat.LRC)
        extensions.forEachIndexed { index, ext ->
            for (name in listOf(ext, ext.uppercase())) {
                val candidate = File(
                    musicFile.parentFile,
                    musicFile.nameWithoutExtension + "." + name
                )
                if (!candidate.isFile) continue
                val text = try {
                    candidate.readBytes().toString(Charset.defaultCharset())
                } catch (_: Exception) {
                    continue
                }
                try {
                    parseLyrics(text, audioMimeType, parserOptions, formats[index])?.let { return it }
                } catch (_: Exception) {
                    // Malformed sidecar: try the next extension.
                }
            }
        }
        return null
    }
}

/**
 * MiniMusic renders one lyrics view, so pick the most useful candidate rather
 * than Gramophone's raw order: word-timed, then line-synced, then plain text.
 * Top level (not inside [LrcUtils]) so callers can import it directly.
 */
fun List<SemanticLyrics>.bestCandidate(): SemanticLyrics? {
    val synced = filterIsInstance<SemanticLyrics.SyncedLyrics>()
    val wordTimed = synced.firstOrNull { line -> line.text.any { it.words != null } }
    return wordTimed ?: synced.firstOrNull() ?: firstOrNull()
}

/**
 * Renders lyrics into the LRC-style text MiniMusic's lyrics screen consumes:
 * one "[mm:ss.xx]<line>" per timed line, or plain lines for unsynced text.
 * Internal newlines in a synced line are flattened to spaces so a single timed
 * line never spills into the screen's untimed-line bucket.
 */
fun SemanticLyrics.toLyricsText(): String? = when (this) {
    is SemanticLyrics.SyncedLyrics -> buildString {
        for (line in text) {
            val body = line.text.replace('\n', ' ').trim()
            if (body.isEmpty()) continue
            val ms = line.start.toLong()
            append(
                "[%02d:%02d.%02d]".format(
                    Locale.US,
                    ms / 60_000,
                    (ms % 60_000) / 1000,
                    (ms % 1000) / 10
                )
            )
            append(body)
            append('\n')
        }
    }.trim().ifEmpty { null }

    is SemanticLyrics.UnsyncedLyrics ->
        unsyncedText.joinToString("\n") { it.first }.trim().ifEmpty { null }
}
