package com.androidprj.fuzic.player.queue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM coverage for the single-shot gate that backs
 * `Media3PlayerRepository`'s auto-skip behaviour.
 *
 * The repository's full flow cannot run in pure JVM because
 * [androidx.media3.session.MediaController] is constructed against an
 * Android `Looper`. The gate itself is pure Kotlin, so we test the
 * shared logic here and rely on manual smoke for the Media3 wiring.
 */
class AutoSkipGateTest {

    @Test
    fun firstTryConsumeSucceeds() {
        val gate = AutoSkipGate()
        assertTrue(gate.tryConsume())
    }

    @Test
    fun secondTryConsumeFails() {
        val gate = AutoSkipGate()
        gate.tryConsume()
        assertFalse(gate.tryConsume())
        assertFalse(gate.tryConsume())
    }

    @Test
    fun resetAllowsAnotherConsume() {
        val gate = AutoSkipGate()
        gate.tryConsume()
        assertFalse(gate.tryConsume())

        gate.reset()
        assertTrue(gate.tryConsume())
        assertFalse(gate.tryConsume())
    }

    @Test
    fun resetWithoutPriorConsumeIsNoOp() {
        val gate = AutoSkipGate()
        gate.reset()
        assertTrue(gate.tryConsume())
    }

    @Test
    fun manyResetsCycleConsumeCorrectly() {
        val gate = AutoSkipGate()
        repeat(5) {
            assertTrue(gate.tryConsume())
            assertFalse(gate.tryConsume())
            gate.reset()
        }
        assertTrue(gate.tryConsume())
    }
}
