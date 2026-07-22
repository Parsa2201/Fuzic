package com.androidprj.fuzic.player.queue

import com.androidprj.fuzic.model.ui.SongItem
import java.util.Random

/**
 * Pure-Kotlin helpers for shuffling a queue of [SongItem]s.
 *
 * The shuffle is deterministic when [Random] is constructed with a fixed
 * seed; pass a seeded `Random` from tests to verify behaviour without
 * relying on global RNG state.
 *
 * No Android or Media3 imports — this object is unit-testable in plain JVM.
 */
object QueueShuffler {

    /**
     * Returns a NEW list containing every input element exactly once, in
     * a Fisher-Yates-permuted order. The input list is never mutated.
     *
     * - Empty input returns empty list.
     * - Single element returns a single-element list.
     * - Time complexity: O(n). Space complexity: O(n).
     *
     * The caller controls determinism by seeding [random]. The default
     * `Random()` is non-deterministic; tests should pass a seeded
     * `Random(seed)`.
     */
    fun shuffled(songs: List<SongItem>, random: Random): List<SongItem> {
        if (songs.isEmpty()) return emptyList()
        // Defensive copy so callers' lists are never mutated, even if a
        // mutable list sneaks in via the public surface.
        val buffer = songs.toMutableList()
        for (i in buffer.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val swap = buffer[i]
            buffer[i] = buffer[j]
            buffer[j] = swap
        }
        // toList() returns an unmodifiable view backed by the buffer; that
        // is fine for our usage but we expose a fresh ArrayList for safety.
        return buffer.toList()
    }

    /**
     * Returns true when [shuffled] differs from [original] in size or any
     * position. Used by tests to confirm a shuffle actually permuted the
     * order (or to assert two shuffles produced the same permutation).
     */
    fun isShuffledDifferent(
        original: List<SongItem>,
        shuffled: List<SongItem>,
    ): Boolean {
        if (original.size != shuffled.size) return true
        for (i in original.indices) {
            if (original[i].id != shuffled[i].id) return true
        }
        return false
    }
}
