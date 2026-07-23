package com.androidprj.fuzic.player.crossfade

/**
 * Discrete-time, linear volume ramp used by [CrossfadeController] to
 * fade the outgoing player 1→0 and the incoming player 0→1 over a
 * configured window.
 *
 * Pure JVM — easy to unit-test without Media3 or Android SDK runtime.
 *
 * ## Usage
 *
 * ```
 * val ramp = VolumeRamp(durationMs = 6_000L, from = 1f, to = 0f, tickIntervalMs = 20L)
 * for (value in ramp) {
 *     outgoingPlayer.volume = value
 *     delay(ramp.tickIntervalMs)
 * }
 * ```
 *
 * ## Why discrete-step instead of `Interpolator`
 *
 * A controller-friendly coroutine flow that ticks every [tickIntervalMs]
 * is easier to cancel on user skip / pause than a smoothing interpolator,
 * and the linear ramp matches how crossfade graphs render in audio
 * players — a straight line from `from` to `to` over the configured
 * window.
 */
class VolumeRamp(
    val durationMs: Long,
    val from: Float,
    val to: Float,
    val tickIntervalMs: Long = DEFAULT_TICK_INTERVAL_MS,
) {

    init {
        require(durationMs > 0L) { "durationMs must be > 0 (was $durationMs)" }
        require(tickIntervalMs > 0L) { "tickIntervalMs must be > 0 (was $tickIntervalMs)" }
        require(from in 0f..1f) { "from must be in [0, 1] (was $from)" }
        require(to in 0f..1f) { "to must be in [0, 1] (was $to)" }
    }

    val stepCount: Int = ((durationMs + tickIntervalMs - 1) / tickIntervalMs).toInt()

    operator fun iterator(): Iterator<Float> = object : Iterator<Float> {
        private var index = 0
        override fun hasNext(): Boolean = index < stepCount
        override fun next(): Float {
            if (!hasNext()) throw NoSuchElementException()
            val progress = index.toFloat() / stepCount.toFloat()
            val v = from + (to - from) * progress
            index += 1
            return v.coerceIn(0f, 1f)
        }
    }

    companion object {
        const val DEFAULT_TICK_INTERVAL_MS: Long = 20L
    }
}
