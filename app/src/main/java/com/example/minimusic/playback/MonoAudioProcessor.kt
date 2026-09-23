package com.example.minimusic.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Downmixes stereo (or multi-channel) PCM to mono by averaging channels.
 * Toggled live via [setEnabled] from the Audio settings; when disabled the
 * processor passes input through untouched.
 *
 * The critical invariant: the OUTPUT format must match the bytes written.
 * While active, [onConfigure] reports a mono output format; while inactive it
 * reports the input format unchanged. A toggle therefore changes the declared
 * format, which is why [setEnabled] calls [flush] — the sink re-reads the
 * configured format on the next stream. Reporting mono while writing stereo
 * bytes (the original bug) made the sink interpret half the samples as
 * channels and playback silently broke.
 */
class MonoAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var enabled: Boolean = false

    private var channelCount = 0

    /** Enables/disables the downmix and reconfigures the stream safely. */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        // Re-run onConfigure so the output format matches the new byte layout.
        flush()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount
        return if (isActive()) inputAudioFormat.copyWithChannelCount(1)
        else inputAudioFormat
    }

    override fun isActive(): Boolean = enabled && channelCount >= 2

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            val out = replaceOutputBuffer(inputBuffer.remaining())
            out.put(inputBuffer).flip()
            return
        }
        val bytesPerFrame = channelCount * 2 // 16-bit PCM
        val out = replaceOutputBuffer(inputBuffer.remaining() / bytesPerFrame * 2)
        while (inputBuffer.remaining() >= bytesPerFrame) {
            var sumL = 0
            var sumR = 0
            for (ch in 0 until channelCount) {
                val sample = inputBuffer.short
                when (ch) {
                    0 -> sumL = sample.toInt()
                    1 -> sumR = sample.toInt()
                    else -> { sumL += sample / 2; sumR += sample / 2 }
                }
            }
            out.putShort(((sumL + sumR) / 2).toShort())
        }
        // Trailing partial frame (shouldn't occur with PCM) passes through.
        while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        out.flip()
    }
}
