package com.androidprj.fuzic.player.queue

/**
 * Single-shot gate controlling auto-skip behaviour in
 * [com.androidprj.fuzic.data.repository.Media3PlayerRepository].
 *
 * ## Contract
 *
 * - `tryConsume()` returns `true` the first time it is called after the
 *   most recent [reset] call. Subsequent calls return `false` until the
 *   gate is reset again.
 * - Thread-safe via a single `@Volatile` field. No locks; safe to call
 *   from the player callback thread and the repository's IO scope
 *   concurrently.
 *
 * ## Why a separate class
 *
 * Extracted from the repository so the "auto-skip on missing source"
 * decision can be unit-tested in plain JVM without instantiating
 * `MediaController` or Robolectric. The repository injects
 * `@Volatile var attempted` via this gate; the test can construct an
 * `AutoSkipGate` directly and verify the consume/reset semantics.
 */
class AutoSkipGate {
    @Volatile
    private var attempted: Boolean = false

    /**
     * Returns `true` if this is the first attempt since the last reset;
     * records the attempt and returns `false` on every subsequent call.
     */
    fun tryConsume(): Boolean {
        if (attempted) return false
        attempted = true
        return true
    }

    /** Clear the gate so the next [tryConsume] can succeed again. */
    fun reset() {
        attempted = false
    }
}
