package com.androidprj.fuzic.player.palette

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM coverage for [dominantColorOf]. The surrounding
 * [DominantColorExtractor] class requires a real Android `Context` for
 * Coil image loading and is verified manually.
 *
 * Test inputs are constructed with raw `0xAARRGGBB` literals and the
 * `rgbArgb` / `argbArgb` helpers so the test does not need to stub
 * `android.graphics.Color`.
 */
class DominantColorExtractorTest {

    @Test
    fun emptyInputReturnsNull() {
        assertNull(dominantColorOf(intArrayOf()))
    }

    @Test
    fun singleRedPixelReturnsRed() {
        val red = rgbArgb(220, 30, 30)
        val pixels = intArrayOf(red)
        assertEquals(red, dominantColorOf(pixels))
    }

    @Test
    fun allGreysReturnsNull() {
        // Saturation 0 for every pixel — grey filter rejects.
        val pixels = intArrayOf(
            rgbArgb(120, 120, 120),
            rgbArgb(180, 180, 180),
            rgbArgb(60, 60, 60),
        )
        assertNull(dominantColorOf(pixels))
    }

    @Test
    fun pureBlackAndPureWhiteAreRejected() {
        // value 0 (black) and value 1 (white) — out of [0.1, 0.95] range.
        assertNull(dominantColorOf(intArrayOf(BLACK_ARGB, WHITE_ARGB)))
    }

    @Test
    fun transparentPixelsAreRejected() {
        // alpha 16 < 32 threshold.
        assertNull(dominantColorOf(
            intArrayOf(
                argbArgb(16, 220, 30, 30),
                argbArgb(0, 220, 30, 30),
            ),
        ))
    }

    @Test
    fun mostSaturatedPixelWins() {
        val pixels = intArrayOf(
            rgbArgb(128, 128, 100), // desaturated
            rgbArgb(220, 30, 30),    // high saturation
            rgbArgb(30, 30, 220),    // high saturation, lower value
        )
        assertEquals(rgbArgb(220, 30, 30), dominantColorOf(pixels))
    }

    @Test
    fun overBrightnessCapRejectsExtremeWhites() {
        // WHITE has value 1.0 → 0.95 cap rejects; dim red keeps the field.
        val darkRed = rgbArgb(64, 5, 5)
        assertEquals(darkRed, dominantColorOf(intArrayOf(WHITE_ARGB, darkRed)))
    }

    @Test
    fun rgbArgbHelperMatchesSpec() {
        // 0xFF A4 1E 12 → red 0xA4, green 0x1E, blue 0x12.
        assertEquals(0xFFA41E12.toInt(), rgbArgb(0xA4, 0x1E, 0x12))
    }
}
