package com.androidprj.fuzic.player

import com.androidprj.fuzic.player.audio.AmplitudeFrameBuffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AmplitudeFrameBufferTest {

    @Test
    fun `push emits the same amplitude frame`() = runTest {
        // Arrange
        val buffer = AmplitudeFrameBuffer()
        val amplitudes = floatArrayOf(0.1f, 0.5f, 1f)

        // Act
        buffer.push(amplitudes)
        val frame = buffer.flow().first()

        // Assert
        assertEquals(amplitudes.toList(), frame.amplitudes)
    }

    @Test
    fun `push copies amplitudes by value`() = runTest {
        // Arrange
        val buffer = AmplitudeFrameBuffer()
        val source = floatArrayOf(0.25f, 0.75f)

        // Act
        buffer.push(source)
        source[0] = 1f
        val frame = buffer.flow().first()

        // Assert
        assertEquals(listOf(0.25f, 0.75f), frame.amplitudes)
    }

    @Test
    fun `push remains non-blocking and retains the newest frames when full`() = runTest {
        // Arrange
        val buffer = AmplitudeFrameBuffer()

        // Act
        repeat(100) { value ->
            buffer.push(floatArrayOf(value.toFloat()))
        }
        val retained = buffer.flow().take(2).toList()

        // Assert
        assertEquals(listOf(98f, 99f), retained.map { it.amplitudes.single() })
    }
}
