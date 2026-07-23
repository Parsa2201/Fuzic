package com.androidprj.fuzic.player.crossfade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for the discrete-time linear [VolumeRamp] used by
 * `CrossfadeController` to fade one player out and the other in. The
 * controller itself is exercised manually because it depends on
 * Media3's `Player` (no Robolectric).
 *
 * Iteration is via the explicit `iterator()` method (since `VolumeRamp`
 * is no longer a `Sequence` to keep the constructor public-API-clean;
 * tests use `iterator().asSequence().toList()`).
 */
class VolumeRampTest {

    @Test
    fun stepCountMatchesDurationOverTick() {
        val ramp = VolumeRamp(
            durationMs = 6_000L,
            from = 1f,
            to = 0f,
            tickIntervalMs = 20L,
        )
        // 6_000 / 20 = 300 exact. Verify.
        assertEquals(300, ramp.stepCount)
    }

    @Test
    fun stepCountRoundsUpPartialTick() {
        val ramp = VolumeRamp(
            durationMs = 1_005L,
            from = 0f,
            to = 1f,
            tickIntervalMs = 20L,
        )
        // (1005 + 19) / 20 = 51.2 → 51 integer division, ceil-like.
        assertEquals(51, ramp.stepCount)
    }

    @Test
    fun valuesStayWithinBounds() {
        val ramp = VolumeRamp(
            durationMs = 1_000L,
            from = 1f,
            to = 0f,
            tickIntervalMs = 20L,
        )
        for (v in ramp) {
            assertTrue("value $v outside [0, 1]", v in 0f..1f)
        }
    }

    private fun rampToList(ramp: VolumeRamp): List<Float> =
        ramp.iterator().asSequence().toList()

    @Test
    fun descendingRampStartsAtFromAndDecreasesMonotonically() {
        val ramp = VolumeRamp(
            durationMs = 1_000L,
            from = 1f,
            to = 0f,
            tickIntervalMs = 50L,
        )
        val values: List<Float> = rampToList(ramp)
        assertEquals(20, values.size)
        assertEquals(1f, values.first(), 0.0001f)
        assertTrue(values.last() < values.first())
        for (i in 1 until values.size) {
            val previous: Float = values[i - 1]
            val current: Float = values[i]
            assertTrue(
                "values[$i]=$current should be <= values[${i - 1}]=$previous",
                current <= previous,
            )
        }
    }

    @Test
    fun ascendingRampStartsAtFromAndIncreasesMonotonically() {
        val ramp = VolumeRamp(
            durationMs = 800L,
            from = 0f,
            to = 1f,
            tickIntervalMs = 40L,
        )
        val values: List<Float> = rampToList(ramp)
        assertEquals(20, values.size)
        assertEquals(0f, values.first(), 0.0001f)
        for (i in 1 until values.size) {
            val previous: Float = values[i - 1]
            val current: Float = values[i]
            assertTrue(
                "ascending violation at i=$i: $current < $previous",
                current >= previous,
            )
        }
    }

    @Test
    fun invalidInputsAreRejected() {
        var threw = false
        try {
            VolumeRamp(durationMs = 0L, from = 0f, to = 1f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)

        threw = false
        try {
            VolumeRamp(durationMs = 1_000L, from = -0.1f, to = 1f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)

        threw = false
        try {
            VolumeRamp(durationMs = 1_000L, from = 0f, to = 1.1f)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun exhaustedIteratorReturnsNoNext() {
        val ramp = VolumeRamp(
            durationMs = 100L,
            from = 0f,
            to = 1f,
            tickIntervalMs = 20L,
        )
        val iter = ramp.iterator()
        repeat(ramp.stepCount) {
            assertTrue(iter.hasNext())
            iter.next()
        }
        assertFalse(iter.hasNext())
        var threw = false
        try {
            iter.next()
        } catch (e: NoSuchElementException) {
            threw = true
        }
        assertTrue(threw)
    }
}
