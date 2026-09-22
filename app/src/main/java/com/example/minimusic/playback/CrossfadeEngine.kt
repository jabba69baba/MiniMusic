package com.example.minimusic.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Linear crossfade between consecutive tracks: the outgoing track's volume ramps
 * down over the last [fadeMs] of the item, implemented as a positional gain
 * AudioProcessor installed in the audio sink chain. The player's own position
 * drives the curve, so no decoder-level duplication is needed for local files.
 *
 * Note: this fades out the end of each track. The incoming track starts at
 * full volume (a "fade-into-next" crossfade); a true overlapping dual-player
 * crossfade can replace this later without changing the settings plumbing.
 */
class CrossfadeEngine(private val positionProvider: () -> Long) : BaseAudioProcessor() {

    private var fadeMs: Long = 0L
    private var durationMs: Long = 0L

    /** Currently configured fade length in ms (0 = disabled); exposed for re-configuration. */
    val currentFadeMs: Long get() = fadeMs

    fun configure(enabledSecondsMs: Long, currentDurationMs: Long) {
        fadeMs = enabledSecondsMs
        durationMs = currentDurationMs
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    override fun isActive(): Boolean = fadeMs > 0 && durationMs > fadeMs * 2

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (!isActive()) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer).flip()
            return
        }
        val positionMs = positionProvider().coerceAtLeast(0L)
        val fadeStart = durationMs - fadeMs
        val gain = if (positionMs < fadeStart) {
            1f
        } else {
            val progress = (positionMs - fadeStart).toFloat() / fadeMs.toFloat()
            1f - min(progress, 1f)
        }
        if (gain >= 1f) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer).flip()
            return
        }
        val out = replaceOutputBuffer(remaining)
        while (input.remaining() >= 2) {
            val sample = input.short
            out.putShort((sample * gain).toInt().toShort())
        }
        // Trailing partial byte should not occur with 16-bit PCM.
        while (input.hasRemaining()) out.put(input.get())
        out.flip()
    }
}
