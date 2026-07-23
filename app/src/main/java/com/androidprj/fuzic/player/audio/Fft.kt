package com.androidprj.fuzic.player.audio

import kotlin.math.cos
import kotlin.math.sin

/**
 * In-place radix-2 Cooley-Tukey Fast Fourier Transform. Pure Kotlin with zero
 * Android imports, so the same code path is exercised by JVM unit tests and
 * the running [AmplitudeAudioProcessor].
 *
 * Both arrays represent the signal's real and imaginary parts. Inputs are
 * interpreted as already windowed floating-point samples; outputs are the
 * standard DFT bins. Real-only inputs should pass an [imag] array of all
 * zeros — the visualizer processor does exactly that.
 */
object Fft {

    /**
     * Performs an in-place radix-2 FFT of size `n = real.size`.
     *
     * Throws [IllegalArgumentException] when [real] and [imag] have mismatched
     * sizes, or when the size is not a positive power of two (smallest valid
     * size is 1 and short-circuits with no work).
     */
    fun transform(real: FloatArray, imag: FloatArray) {
        val n = real.size
        require(n == imag.size) {
            "real and imag arrays must have the same size (was " +
                "${real.size} vs ${imag.size})"
        }
        require(n > 0 && (n and (n - 1)) == 0) {
            "size must be a positive power of two (was $n)"
        }
        if (n == 1) return

        val logN = Integer.numberOfTrailingZeros(n)

        // 1. Bit-reversal permutation.
        for (i in 1 until n) {
            val j = reverseBits(i, logN)
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        // 2. Cooley-Tukey butterflies, doubling size each stage.
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val phaseStep = -2.0 * Math.PI / size
            var blockStart = 0
            while (blockStart < n) {
                for (i in 0 until halfSize) {
                    val angle = phaseStep * i
                    val wr = cos(angle).toFloat()
                    val wi = sin(angle).toFloat()
                    val a = blockStart + i
                    val b = a + halfSize
                    val tre = wr * real[b] - wi * imag[b]
                    val tim = wr * imag[b] + wi * real[b]
                    real[b] = real[a] - tre
                    imag[b] = imag[a] - tim
                    real[a] += tre
                    imag[a] += tim
                }
                blockStart += size
            }
            size = size shl 1
        }
    }

    /**
     * Reverses the low [bits] bits of [value] and returns the result. Helper
     * for the bit-reversal permutation step above.
     */
    private fun reverseBits(value: Int, bits: Int): Int {
        var v = value
        var r = 0
        var remaining = bits
        while (remaining > 0) {
            r = (r shl 1) or (v and 1)
            v = v ushr 1
            remaining--
        }
        return r
    }
}
