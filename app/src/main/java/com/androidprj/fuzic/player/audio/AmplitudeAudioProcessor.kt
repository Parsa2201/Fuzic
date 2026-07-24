package com.androidprj.fuzic.player.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 32-band PCM visualizer. Extends [BaseAudioProcessor] so the existing
 * `replaceOutputBuffer` machinery feeds bytes from input to output verbatim
 * — adding this processor to the chain never changes audible audio.
 *
 * ## How it works
 *
 * 1. [queueInput] drains 16-bit PCM frames, multiplies each sample by the
 *    precomputed [HANN] tap into a 1024-slot [ringBuffer], and copies every
 *    input byte to the output buffer. Mono and stereo inputs are handled; for
 *    stereo the two channels are averaged into one ring slot.
 * 2. When [ringBuffer] is full, [runFftAndEmit] runs an in-place radix-2 FFT
 *    via [Fft], takes the first 32 non-redundant bins (1..32, skipping DC),
 *    applies a responsive perceptual scale, emphasizes stronger frequency
 *    bands, smooths against the previous frame with `alpha = 0.4`, and pushes
 *    the bar values via
 *    [AmplitudeFrameBuffer.push].
 *
 * ## Audio thread safety
 *
 * The audio processor is only ever invoked from ExoPlayer's audio thread.
 * [AmplitudeFrameBuffer.push] uses [Channel.trySend] (non-blocking), so
 * [queueInput] never suspends. No locks needed inside [runFftAndEmit].
 *
 * ## Inactive fallback
 *
 * [onConfigure] returns [AudioFormat.NOT_SET] for anything that isn't
 * 16-bit PCM, mono/stereo, and 8–48 kHz sample rate. Inactive processors are
 * pure pass-through and cannot break playback if a stream format drifts.
 */
@OptIn(UnstableApi::class)
class AmplitudeAudioProcessor(
    private val frameBuffer: AmplitudeFrameBuffer,
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT &&
            inputAudioFormat.channelCount in ACCEPTABLE_CHANNELS &&
            inputAudioFormat.sampleRate in ACCEPTABLE_SAMPLE_RATES
        ) {
            inputAudioFormat // pass-through
        } else {
            AudioFormat.NOT_SET
        }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesAvailable = inputBuffer.remaining()
        if (bytesAvailable <= 0) return
        val outputBuffer = replaceOutputBuffer(bytesAvailable)

        // inputAudioFormat is guaranteed non-NOT_SET whenever queueInput
        // runs by BaseAudioProcessor's lifecycle contract.
        val channels = inputAudioFormat.channelCount
        val bytesPerFrame = BYTES_PER_PCM_SAMPLE * channels

        while (inputBuffer.remaining() >= bytesPerFrame) {
            when (channels) {
                MONO_CHANNELS -> {
                    val sample = inputBuffer.getShort()
                    outputBuffer.putShort(sample)
                    ringBuffer[ringWriteIndex] =
                        sample.toFloat() * INVERSE_SHORT_MAX * HANN[ringWriteIndex]
                }
                STEREO_CHANNELS -> {
                    val left = inputBuffer.getShort()
                    val right = inputBuffer.getShort()
                    outputBuffer.putShort(left)
                    outputBuffer.putShort(right)
                    val mono = ((left.toInt() + right.toInt()) * 0.5f) * INVERSE_SHORT_MAX
                    ringBuffer[ringWriteIndex] = mono * HANN[ringWriteIndex]
                }
                else -> {
                    // onConfigure gates this, but if some other processor in
                    // the chain forwards a different channelCount through
                    // us, advance past the frame so we never loop forever.
                    inputBuffer.position(inputBuffer.position() + bytesPerFrame)
                }
            }
            ringWriteIndex++
            if (ringWriteIndex >= FFT_SIZE) {
                runFftAndEmit()
                ringWriteIndex = 0
            }
        }
        // Media3's BaseAudioProcessor contract: subclass must flip the output
        // buffer so getOutput() returns the bytes just written. Without this
        // call getOutput() reads zero bytes.
        outputBuffer.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        ringWriteIndex = 0
        previousBands.fill(0f)
        hasPrevious = false
    }

    override fun onReset() {
        ringWriteIndex = 0
        previousBands.fill(0f)
        hasPrevious = false
    }

    /**
     * Runs an FFT over the current [ringBuffer], derives the first
     * [BAR_COUNT] non-redundant magnitudes, normalizes/smooths them, and
     * hands the result to [AmplitudeFrameBuffer.push]. Called whenever the
     * ring buffer fills; allocates two [FFT_SIZE]-sized arrays per call to
     * keep the transform inputs separable from the ring.
     */
    private fun runFftAndEmit() {
        val real = FloatArray(FFT_SIZE).also {
            System.arraycopy(ringBuffer, 0, it, 0, FFT_SIZE)
        }
        val imag = FloatArray(FFT_SIZE)
        Fft.transform(real, imag)

        val magnitudeScale = 2f / FFT_SIZE.toFloat()
        val out = FloatArray(BAR_COUNT)
        for (i in 0 until BAR_COUNT) {
            val bin = i + 1 // Skip the DC bin at index 0.
            val re = real[bin]
            val im = imag[bin]
            val magnitude = sqrt(re * re + im * im) * magnitudeScale
            // The gain makes quiet musical details visible. The quadratic
            // component gives genuinely strong frequency bands more presence
            // than low-level background signal, while the clamp keeps the UI
            // visualizer inside its maximum height.
            val perceptual = log10(1f + magnitude * VISUALIZER_GAIN)
            val raw = (perceptual + perceptual * perceptual * PEAK_EMPHASIS)
                .coerceIn(0f, 1f)
            val smoothed =
                if (hasPrevious) {
                    SMOOTHING_ALPHA * raw + (1f - SMOOTHING_ALPHA) * previousBands[i]
                } else {
                    raw
                }
            out[i] = smoothed
            previousBands[i] = smoothed
        }
        hasPrevious = true
        frameBuffer.push(out)
    }

    // ── Reusable state. ───────────────────────────────────────────────────
    private val ringBuffer: FloatArray = FloatArray(FFT_SIZE)
    private var ringWriteIndex: Int = 0
    private val previousBands: FloatArray = FloatArray(BAR_COUNT)
    private var hasPrevious: Boolean = false

    companion object {
        // Bar count matches the visualizer UI's
        // `internal const val MAX_VISUALIZER_BARS = 32` so the UI can render
        // without resampling.
        private const val BAR_COUNT = 32

        // FFT size: 1024 samples ≈ 23 ms at 44.1 kHz — about the cadence
        // the spec calls "approximately 30 Hz".
        private const val FFT_SIZE = 1024
        private const val BYTES_PER_PCM_SAMPLE = 2
        private const val INVERSE_SHORT_MAX = 1f / Short.MAX_VALUE.toFloat()
        private const val SMOOTHING_ALPHA = 0.4f
        private const val VISUALIZER_GAIN = 18f
        private const val PEAK_EMPHASIS = 0.55f
        private const val MONO_CHANNELS = 1
        private const val STEREO_CHANNELS = 2
        private val ACCEPTABLE_CHANNELS: IntRange = 1..2
        private val ACCEPTABLE_SAMPLE_RATES: IntRange = 8_000..48_000

        /**
         * Pre-computed Hann window of length [FFT_SIZE]. The window tapers
         * the PCM buffer at both ends so the FFT doesn't see a hard
         * discontinuity (which would smear the spectrum). Pre-computed once
         * at class load — the window never changes.
         */
        private val HANN: FloatArray = FloatArray(FFT_SIZE).also { taps ->
            for (i in 0 until FFT_SIZE) {
                val angle = 2.0 * Math.PI * i / (FFT_SIZE - 1)
                taps[i] = (0.5 * (1.0 - cos(angle))).toFloat()
            }
        }
    }
}
