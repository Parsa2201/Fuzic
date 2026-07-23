package com.androidprj.fuzic.player

import com.androidprj.fuzic.player.timer.SleepTimer
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    @Test
    fun `start expires once after exactly one minute`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        var expirationCount = 0
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }

        // Act
        timer.start(1)
        runCurrent()
        advanceTimeBy(59_999L)
        runCurrent()

        // Assert
        assertEquals(0, expirationCount)
        assertTrue(timer.isActive)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(1, expirationCount)
        assertFalse(timer.isActive)
    }

    @Test
    fun `starting a new timer cancels the previous timer`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        var expirationCount = 0
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }

        // Act
        timer.start(2)
        runCurrent()
        advanceTimeBy(30_000L)
        timer.start(1)
        runCurrent()
        advanceTimeBy(60_000L)
        runCurrent()
        advanceUntilIdle()

        // Assert
        assertEquals(1, expirationCount)
    }

    @Test
    fun `cancel prevents an active timer from expiring`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        var expirationCount = 0
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }
        timer.start(1)
        runCurrent()

        // Act
        timer.cancel()
        advanceUntilIdle()

        // Assert
        assertEquals(0, expirationCount)
        assertFalse(timer.isActive)
    }

    @Test
    fun `start with null cancels without expiring`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        var expirationCount = 0
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }
        timer.start(1)
        runCurrent()

        // Act
        timer.start(null)
        advanceUntilIdle()

        // Assert
        assertEquals(0, expirationCount)
        assertFalse(timer.isActive)
    }

    @Test
    fun `start with zero cancels without expiring`() = runTest {
        // Arrange
        var expirationCount = 0
        val dispatcher = StandardTestDispatcher(testScheduler)
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }

        // Act
        timer.start(0)
        advanceUntilIdle()

        // Assert
        assertEquals(0, expirationCount)
        assertFalse(timer.isActive)
    }

    @Test
    fun `start with a negative duration cancels without expiring`() = runTest {
        // Arrange
        var expirationCount = 0
        val dispatcher = StandardTestDispatcher(testScheduler)
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            expirationCount++
        }

        // Act
        timer.start(-1)
        advanceUntilIdle()

        // Assert
        assertEquals(0, expirationCount)
        assertFalse(timer.isActive)
    }

    @Test
    fun `isActive tracks expiry and cancellation`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {}

        // Act and assert expiry
        timer.start(1)
        assertTrue(timer.isActive)
        advanceTimeBy(60_000L)
        runCurrent()
        assertFalse(timer.isActive)

        // Act and assert cancellation
        timer.start(1)
        assertTrue(timer.isActive)
        timer.cancel()
        assertFalse(timer.isActive)
    }

    @Test
    fun `expiration failure is reported from the provided test dispatcher`() = runTest {
        // Arrange
        val dispatcher = StandardTestDispatcher(testScheduler)
        val failure = IllegalStateException("expiration failed")
        var observedDispatcher: CoroutineDispatcher? = null
        var expirationResult: Result<Unit>? = null
        val timer = SleepTimer(CoroutineScope(coroutineContext + dispatcher)) {
            observedDispatcher = currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
            throw failure
        }

        // Act
        timer.start(1) { expirationResult = it }
        advanceTimeBy(60_000L)
        runCurrent()

        // Assert
        assertSame(dispatcher, observedDispatcher)
        assertSame(failure, expirationResult?.exceptionOrNull())
        assertFalse(timer.isActive)
    }
}
