package com.androidprj.fuzic.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.androidprj.fuzic.player.audio.AmplitudeAudioProcessor
import com.androidprj.fuzic.player.audio.AmplitudeFrameBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class AmplitudeAudioProcessorTest {

    @Test
    fun `queueInput preserves PCM bytes across repeated output drains`() {
        // Arrange
        val processor = configuredProcessor(AmplitudeFrameBuffer())
        val input = sinePcm(frequencyHz = 440)

        // Act
        val output = queueInChunksAndDrain(processor, input)

        // Assert
        assertArrayEquals(input, output)
    }

    @Test
    fun `a 440 hertz sine emits normalized amplitudes around its FFT bin`() = runTest {
        // Arrange
        val frameBuffer = AmplitudeFrameBuffer()
        val processor = configuredProcessor(frameBuffer)
        val input = sinePcm(frequencyHz = 440)

        // Act
        processor.queueInput(directBuffer(input))
        drainOutput(processor)
        val frame = frameBuffer.flow().first()

        // Assert
        val expectedFftBin = 440 * FFT_SIZE / SAMPLE_RATE
        val expectedFrameIndex = expectedFftBin - 1 // Frames skip the DC bin.
        val dominantIndex = frame.amplitudes.indices.maxByOrNull { frame.amplitudes[it] }
            ?: error("Expected a non-empty amplitude frame")
        assertEquals(BAR_COUNT, frame.amplitudes.size)
        assertTrue(frame.amplitudes.all { it in 0f..1f })
        assertEquals(expectedFrameIndex, dominantIndex)
        assertTrue(frame.amplitudes[expectedFrameIndex] >= 0.4f)
    }

    @Test
    fun `different sine frequencies peak near their corresponding FFT bins`() = runTest {
        // Arrange
        val frequencies = listOf(220, 880)

        frequencies.forEach { frequency ->
            val frameBuffer = AmplitudeFrameBuffer()
            val processor = configuredProcessor(frameBuffer)

            // Act
            processor.queueInput(directBuffer(sinePcm(frequency)))
            drainOutput(processor)
            val amplitudes = frameBuffer.flow().first().amplitudes

            // Assert
            val expectedFrameIndex = frequency * FFT_SIZE / SAMPLE_RATE - 1
            val dominantIndex = amplitudes.indices.maxByOrNull { amplitudes[it] }
                ?: error("Expected a non-empty amplitude frame")
            assertTrue(dominantIndex in expectedFrameIndex..(expectedFrameIndex + 1))
        }
    }

    @Test
    fun `reset clears smoothing from the previous amplitude frame`() = runTest {
        // Arrange
        val frameBuffer = AmplitudeFrameBuffer()
        val processor = configuredProcessor(frameBuffer)
        processor.queueInput(directBuffer(sinePcm(440)))
        drainOutput(processor)
        frameBuffer.flow().first()

        // Act
        processor.queueInput(directBuffer(ByteArray(FFT_SIZE * PCM_BYTES_PER_SAMPLE)))
        drainOutput(processor)
        val smoothedSilence = frameBuffer.flow().first()
        processor.reset()
        configure(processor)
        processor.queueInput(directBuffer(ByteArray(FFT_SIZE * PCM_BYTES_PER_SAMPLE)))
        drainOutput(processor)
        val resetSilence = frameBuffer.flow().first()

        // Assert
        assertTrue(smoothedSilence.amplitudes.any { it > 0f })
        assertTrue(resetSilence.amplitudes.all { it == 0f })
    }

    @Test
    fun `unsupported PCM input leaves the processor inactive`() {
        // Arrange
        val processor = AmplitudeAudioProcessor(AmplitudeFrameBuffer())
        val unsupported = AudioProcessor.AudioFormat(
            SAMPLE_RATE,
            MONO_CHANNELS,
            C.ENCODING_PCM_FLOAT,
        )

        // Act
        val outputFormat = processor.configure(unsupported)

        // Assert
        assertEquals(AudioProcessor.AudioFormat.NOT_SET, outputFormat)
        assertFalse(processor.isActive)
    }

    private fun configuredProcessor(frameBuffer: AmplitudeFrameBuffer): AmplitudeAudioProcessor =
        AmplitudeAudioProcessor(frameBuffer).also(::configure)

    private fun configure(processor: AmplitudeAudioProcessor) {
        val format = AudioProcessor.AudioFormat(
            SAMPLE_RATE,
            MONO_CHANNELS,
            C.ENCODING_PCM_16BIT,
        )
        processor.configure(format)
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
    }

    private fun queueInChunksAndDrain(
        processor: AmplitudeAudioProcessor,
        input: ByteArray,
    ): ByteArray {
        val output = ByteArrayOutputStream(input.size)
        var offset = 0
        while (offset < input.size) {
            val byteCount = minOf(CHUNK_BYTES, input.size - offset)
            processor.queueInput(directBuffer(input, offset, byteCount))
            val chunk = drainOutput(processor)
            output.write(chunk, 0, chunk.size)
            offset += byteCount
        }
        return output.toByteArray()
    }

    private fun drainOutput(processor: AmplitudeAudioProcessor): ByteArray {
        val output = processor.getOutput()
        return ByteArray(output.remaining()).also { bytes -> output.get(bytes) }
    }

    private fun directBuffer(
        bytes: ByteArray,
        offset: Int = 0,
        byteCount: Int = bytes.size,
    ): ByteBuffer = ByteBuffer.allocateDirect(byteCount)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            put(bytes, offset, byteCount)
            flip()
        }

    private fun sinePcm(frequencyHz: Int): ByteArray {
        val bytes = ByteArray(FFT_SIZE * PCM_BYTES_PER_SAMPLE)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        repeat(FFT_SIZE) { sampleIndex ->
            val angle = 2.0 * PI * frequencyHz * sampleIndex / SAMPLE_RATE
            val sample = (sin(angle) * Short.MAX_VALUE * SINE_AMPLITUDE).roundToInt().toShort()
            buffer.putShort(sample)
        }
        return bytes
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val MONO_CHANNELS = 1
        const val FFT_SIZE = 1_024
        const val BAR_COUNT = 32
        const val PCM_BYTES_PER_SAMPLE = 2
        const val CHUNK_BYTES = 256
        const val SINE_AMPLITUDE = 0.9
    }
}
