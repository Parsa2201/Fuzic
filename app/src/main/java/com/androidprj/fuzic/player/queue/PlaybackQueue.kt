package com.androidprj.fuzic.player.queue

import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import java.util.Random

/**
 * Immutable playback queue state machine.
 *
 * ## Contract
 *
 * - **Pure Kotlin.** No Android, Media3, Compose, Coroutine, or Hilt imports.
 *   This class is unit-testable in plain JVM.
 * - **Immutable.** Every mutator returns a NEW [PlaybackQueue] instance;
 *   the receiver is never modified. Two snapshots can diverge safely.
 * - **Deterministic when seeded.** Pass [randomSeed] to make shuffles
 *   reproducible; otherwise a system-time seed is used.
 *
 * ## State semantics
 *
 * - [originalSongs] is the canonical, un-shuffled order. It survives
 *   [withShuffleEnabled] toggles so the user can switch shuffle off and
 *   get the original order back.
 * - [songs] is the *current* playback order. When [shuffleEnabled] is true
 *   this is a stable permutation of [originalSongs]; otherwise it equals
 *   [originalSongs].
 * - [currentIndex] is the playhead position into [songs]. `-1` means the
 *   queue is empty and there is no current song.
 * - [repeatMode] controls how [next] / [previous] advance at boundaries.
 *
 * ## Repeat semantics
 *
 * - [RepeatMode.Off]: [next] stops at the last index; [previous] stops at
 *   index 0. The boundary index stays put (no wrap-around).
 * - [RepeatMode.All]: [next] / [previous] wrap around the queue.
 * - [RepeatMode.One]: [next] / [previous] are no-ops; the same song
 *   repeats. Transport-level re-triggering is the caller's responsibility.
 */
data class PlaybackQueue(
    val originalSongs: List<SongItem>,
    val songs: List<SongItem>,
    val currentIndex: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
) {

    /**
     * Convenience constructor that builds the canonical state directly
     * from an initial list of songs.
     *
     * @param initialSongs The un-shuffled starting list.
     * @param initialIndex The starting playhead; coerced into
     *  `[0, initialSongs.lastIndex]` (or `-1` for empty input).
     * @param initialShuffleEnabled When true the initial songs are
     *  shuffled with the supplied [randomSeed].
     * @param initialRepeatMode Starting repeat mode.
     * @param randomSeed Seed for the shuffle RNG. `null` uses
     *  `System.nanoTime()` — pass a fixed value in tests for determinism.
     */
    constructor(
        initialSongs: List<SongItem> = emptyList(),
        initialIndex: Int = 0,
        initialShuffleEnabled: Boolean = false,
        initialRepeatMode: RepeatMode = RepeatMode.Off,
        randomSeed: Long? = null,
    ) : this(
        originalSongs = initialSongs,
        songs = computeInitialSongs(initialSongs, initialShuffleEnabled, randomSeed),
        currentIndex = computeInitialIndex(initialSongs, initialIndex),
        shuffleEnabled = initialShuffleEnabled,
        repeatMode = initialRepeatMode,
    )

    private companion object {
        fun computeInitialSongs(
            initialSongs: List<SongItem>,
            initialShuffleEnabled: Boolean,
            randomSeed: Long?,
        ): List<SongItem> {
            if (!initialShuffleEnabled || initialSongs.isEmpty()) return initialSongs
            return QueueShuffler.shuffled(
                initialSongs,
                Random(randomSeed ?: System.nanoTime()),
            )
        }

        fun computeInitialIndex(
            initialSongs: List<SongItem>,
            initialIndex: Int,
        ): Int =
            if (initialSongs.isEmpty()) -1
            else initialIndex.coerceIn(0, initialSongs.lastIndex)
    }

    /** Currently playing song, or null when the queue is empty. */
    val currentSong: SongItem?
        get() = songs.getOrNull(currentIndex)

    /** Next song the player will move to, honouring [repeatMode]. */
    val nextSong: SongItem?
        get() {
            if (songs.isEmpty()) return null
            return when (repeatMode) {
                RepeatMode.One -> currentSong
                RepeatMode.All -> songs[(currentIndex + 1).mod(songs.size)]
                RepeatMode.Off -> songs.getOrNull(currentIndex + 1)
            }
        }

    /** Previous song the player will move to, honouring [repeatMode]. */
    val previousSong: SongItem?
        get() {
            if (songs.isEmpty()) return null
            return when (repeatMode) {
                RepeatMode.One -> currentSong
                RepeatMode.All -> songs[(currentIndex - 1).mod(songs.size)]
                RepeatMode.Off -> songs.getOrNull(currentIndex - 1)
            }
        }

    /**
     * Replaces the queue with [songs]. The new list becomes
     * [originalSongs] verbatim; [songs] gets re-shuffled if
     * [shuffleEnabled] is currently true, with the song at
     * [startIndex] pinned to position 0 so the user's requested song is
     * guaranteed to be the new current song.
     *
     * Binary outcomes:
     * - Empty input → empty queue, `currentIndex = -1`.
     * - Negative or oversized [startIndex] → coerced into
     *   `[0, songs.lastIndex]`.
     */
    fun withSongs(
        songs: List<SongItem>,
        startIndex: Int = 0,
    ): PlaybackQueue {
        if (songs.isEmpty()) {
            return copy(
                originalSongs = emptyList(),
                songs = emptyList(),
                currentIndex = -1,
            )
        }
        val safeStart = startIndex.coerceIn(0, songs.lastIndex)
        val (orderedOriginal, orderedSongs) = if (shuffleEnabled) {
            val anchor = songs[safeStart]
            val rest = songs.filterIndexed { index, _ -> index != safeStart }
            val shuffledRest = QueueShuffler.shuffled(rest, Random())
            songs to (listOf(anchor) + shuffledRest)
        } else {
            songs to songs
        }
        // The anchor sits at position 0 in the shuffled view, so the new
        // playhead is always 0 for the shuffle-on path; otherwise it is
        // the requested index.
        val newIndex = if (shuffleEnabled) 0 else safeStart
        return copy(
            originalSongs = orderedOriginal,
            songs = orderedSongs,
            currentIndex = newIndex,
        )
    }

    /**
     * Appends [song] to both the original and current ordering. The
     * playhead does NOT move; if the user was on the last song they
     * remain on it (now one earlier in the list).
     */
    fun withAddedSong(song: SongItem): PlaybackQueue = copy(
        originalSongs = originalSongs + song,
        songs = songs + song,
    )

    /**
     * Removes the song with [songId] from both the original and current
     * ordering. When the current song is removed, the playhead moves to
     * the same numerical index so the *next* song becomes current.
     * Removes a missing ID is a no-op (queue unchanged).
     *
     * Binary outcomes:
     * - Last song removed → empty queue, `currentIndex = -1`.
     * - Current song removed and is the last item → playhead stays on
     *   the new last index (or `-1` when empty).
     */
    fun withRemovedSong(songId: String): PlaybackQueue {
        val removedInSongs = songs.indexOfFirst { it.id == songId }
        val newSongs = if (removedInSongs >= 0) {
            songs.toMutableList().also { it.removeAt(removedInSongs) }
        } else {
            songs
        }
        val newOriginal = if (originalSongs.any { it.id == songId }) {
            originalSongs.filterNot { it.id == songId }
        } else {
            originalSongs
        }
        val newIndex = when {
            newSongs.isEmpty() -> -1
            removedInSongs < 0 -> currentIndex.coerceIn(0, newSongs.lastIndex)
            removedInSongs < currentIndex -> currentIndex - 1
            removedInSongs == currentIndex -> currentIndex.coerceAtMost(newSongs.lastIndex)
            else -> currentIndex.coerceAtMost(newSongs.lastIndex)
        }
        return copy(
            originalSongs = newOriginal,
            songs = newSongs,
            currentIndex = newIndex,
        )
    }

    /** Empties the queue and resets the playhead to `-1`. */
    fun withCleared(): PlaybackQueue = copy(
        originalSongs = emptyList(),
        songs = emptyList(),
        currentIndex = -1,
    )

    /**
     * Toggles shuffle. On enable, [originalSongs] is re-shuffled with the
     * current song pinned at position 0; on disable, [songs] is restored
     * from [originalSongs] and the current song's new index is computed.
     *
     * No-op when [enabled] equals the current [shuffleEnabled].
     */
    fun withShuffleEnabled(enabled: Boolean): PlaybackQueue {
        if (enabled == shuffleEnabled) return this
        if (enabled) {
            // Enable: shuffle originalSongs, anchor current song at index 0.
            val anchor = currentSong
            val pool = originalSongs.filterNot { song -> anchor != null && song.id == anchor.id }
            val shuffledRest = if (pool.isEmpty()) emptyList() else QueueShuffler.shuffled(pool, Random())
            val newSongs = if (anchor != null) listOf(anchor) + shuffledRest else shuffledRest
            val newIndex = if (newSongs.isEmpty()) -1 else 0
            return copy(
                shuffleEnabled = true,
                songs = newSongs,
                currentIndex = newIndex,
            )
        }
        // Disable: restore canonical order; find current song's new index.
        val anchor = currentSong
        val restoredIndex = if (anchor == null) {
            -1
        } else {
            originalSongs.indexOfFirst { it.id == anchor.id }
        }
        return copy(
            shuffleEnabled = false,
            songs = originalSongs,
            currentIndex = restoredIndex,
        )
    }

    fun withRepeatMode(mode: RepeatMode): PlaybackQueue = copy(repeatMode = mode)

    /**
     * Sets the playhead to [index]. Values outside `[0, songs.lastIndex]`
     * are clamped; empty queues always yield `-1`.
     */
    fun withIndexMovedTo(index: Int): PlaybackQueue = copy(
        currentIndex = when {
            songs.isEmpty() -> -1
            index < 0 -> 0
            index > songs.lastIndex -> songs.lastIndex
            else -> index
        },
    )

    /**
     * Advances the playhead one slot, honouring [repeatMode]. No-op on
     * an empty queue.
     */
    fun next(): PlaybackQueue {
        if (songs.isEmpty()) return this
        return when (repeatMode) {
            RepeatMode.One -> this
            RepeatMode.All -> copy(currentIndex = (currentIndex + 1).mod(songs.size))
            RepeatMode.Off -> copy(
                currentIndex = (currentIndex + 1).coerceAtMost(songs.lastIndex),
            )
        }
    }

    /**
     * Steps the playhead back one slot, honouring [repeatMode]. No-op on
     * an empty queue.
     */
    fun previous(): PlaybackQueue {
        if (songs.isEmpty()) return this
        return when (repeatMode) {
            RepeatMode.One -> this
            RepeatMode.All -> copy(currentIndex = (currentIndex - 1).mod(songs.size))
            RepeatMode.Off -> copy(
                currentIndex = (currentIndex - 1).coerceAtLeast(0),
            )
        }
    }
}
