package com.example.minimusic.data.lyrics

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput

/**
 * Reads a song's container metadata without playing it, using Media3's own
 * extractors — the same battle-tested tag readers ExoPlayer uses during
 * playback. This is how Gramophone gets lyrics to always show up: instead of
 * hand-rolling ID3/FLAC/MP4 byte walking, the file's own extractor emits the
 * ID3 (SYLT/USLT), FLAC Vorbis comment, MP4 "©lyr", and Ogg comment entries into
 * the track [Format], and [LrcUtils.extractAndParseLyrics] decodes them.
 *
 * The pass stops at the first track format, which is where every supported
 * container has already delivered its tag metadata, so a 100 MB album costs a
 * header read, not a full scan.
 */
internal object EmbeddedMetadataReader {

    /** Tag metadata plus the audio format values [LrcUtils] needs. */
    data class AudioMetadata(
        val sampleRate: Int,
        val sampleMimeType: String?,
        val metadata: Metadata
    )

    /** Reads [uri] (content:// or file) through a [DefaultDataSource]. */
    fun read(context: Context, uri: Uri): AudioMetadata? = runCatching {
        // Second argument is allowCrossProtocolNetworkAccess; local media never
        // needs it, and content:// URIs resolve through the resolver instead.
        val dataSource = DefaultDataSource(context, false)
        try {
            readFrom(dataSource, DataSpec.Builder().setUri(uri).build())
        } finally {
            runCatching { dataSource.close() }
        }
    }.getOrNull()

    /**
     * Sniffs [dataSpec] with [DefaultExtractorsFactory], then drives the winning
     * extractor until it emits a track format. Exposed with an injectable
     * [DataSource] so JVM unit tests can feed a synthetic file.
     */
    internal fun readFrom(dataSource: DataSource, dataSpec: DataSpec): AudioMetadata? {
        dataSource.open(dataSpec)
        try {
            var input = DefaultExtractorInput(
                dataSource,
                dataSpec.position,
                dataSpec.length
            )
            val extractor = sniff(input) ?: return null
            val output = CapturingOutput()
            try {
                extractor.init(output)
                val seekPosition = PositionHolder()
                var result: Int
                var reads = 0
                do {
                    result = extractor.read(input, seekPosition)
                    if (result == Extractor.RESULT_SEEK) {
                        // An extractor can ask to move (e.g. an MP4 file whose
                        // moov atom sits at the end). DefaultExtractorInput
                        // cannot move, so re-point the stream and hand the
                        // extractor a fresh input at the requested position.
                        val target = seekPosition.position
                        runCatching { dataSource.close() }
                        dataSource.open(dataSpec.buildUpon().setPosition(target).build())
                        val remaining =
                            if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                                C.LENGTH_UNSET.toLong()
                            } else {
                                dataSpec.length - target
                            }
                        input = DefaultExtractorInput(dataSource, target, remaining)
                        extractor.seek(target, C.TIME_UNSET)
                    }
                    reads++
                    // First format = tags delivered for every container we care
                    // about; nothing later in the file adds lyrics.
                } while (result == Extractor.RESULT_CONTINUE &&
                    output.formats.isEmpty() &&
                    reads < MAX_READS
                )
            } finally {
                runCatching { extractor.release() }
            }
            return output.toAudioMetadata()
        } finally {
            runCatching { dataSource.close() }
        }
    }

    private const val MAX_READS = 5_000

    private fun sniff(input: DefaultExtractorInput): Extractor? {
        for (candidate in DefaultExtractorsFactory().createExtractors()) {
            input.resetPeekPosition()
            val matched = try {
                candidate.sniff(input)
            } catch (_: Exception) {
                false
            }
            if (matched) return candidate
        }
        return null
    }

    private class CapturingOutput : ExtractorOutput {
        val formats = mutableListOf<Format>()

        /** Drain buffer for [sampleData]; never retained. */
        private val scratch = ByteArray(8 * 1024)

        private val capture = object : TrackOutput {
            override fun format(format: Format) {
                formats += format
            }

            // Samples are drained rather than kept: forwarding them keeps the
            // underlying stream in sync for any further extractor reads, while
            // nothing is retained because only tag metadata matters here.
            // DataReader exposes no skip, so bytes go through a scratch buffer.
            override fun sampleData(data: DataReader, length: Int, allowEndOfInput: Boolean, part: Int): Int {
                var remaining = length
                while (remaining > 0) {
                    val toRead = minOf(remaining, scratch.size)
                    val read = data.read(scratch, 0, toRead)
                    if (read == C.RESULT_END_OF_INPUT) {
                        return if (allowEndOfInput) C.RESULT_END_OF_INPUT else length - remaining
                    }
                    if (read <= 0) break
                    remaining -= read
                }
                return length - remaining
            }

            override fun sampleData(data: ParsableByteArray, length: Int, part: Int) {
                data.skipBytes(length)
            }

            override fun sampleMetadata(
                timeUs: Long,
                flags: Int,
                offset: Int,
                size: Int,
                cryptoData: TrackOutput.CryptoData?
            ) = Unit
        }

        override fun track(trackId: Int, type: Int): TrackOutput = capture

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) = Unit

        fun toAudioMetadata(): AudioMetadata? {
            val audioFormat = formats.firstOrNull { it.sampleMimeType?.startsWith("audio/") == true }
                ?: formats.firstOrNull()
                ?: return null
            var merged = Metadata()
            for (format in formats) {
                format.metadata?.let { merged = merged.copyWithAppendedEntriesFrom(it) }
            }
            if (merged.length() == 0) return null
            return AudioMetadata(
                sampleRate = audioFormat.sampleRate.takeIf { it > 0 } ?: 0,
                sampleMimeType = audioFormat.sampleMimeType,
                metadata = merged
            )
        }
    }
}
