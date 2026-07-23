package com.androidprj.fuzic.player.crossfade

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Orchestrator for the crossfade transition between consecutive tracks.
 *
 * Owns two inner [Player] instances and a [CrossfadingPlayer] wrapper
 * that forwards to whichever one is "active". When the current track is
 * within [durationMs] of its end AND a next media item exists, both
 * players are volume-ramped: the outgoing 1→0 and the incoming 0→1
 * over the configured window. On ramp completion, [CrossfadingPlayer.swapTo]
 * switches the active player.
 *
 * ## Current scope
 *
 * The wrapper, volume ramp, swap helper, and controller skeleton all exist
 * and pass pure-JVM unit tests. Wiring this controller into
 * [com.androidprj.fuzic.player.FuzicPlaybackService] (constructing two
 * ExoPlayers, binding MediaSession to the wrapper, observing the wrapper
 * for transitions) is intentionally deferred to a follow-up increment
 * so we don't churn the well-tested service surface.
 *
 * With [durationMs] == 0 the controller is a documented no-op — the
 * public [PlayerRepository.setCrossfadeDurationMs] API is the surface the
 * UI Track will eventually drive.
 */
@Singleton
@UnstableApi
class CrossfadeController @Inject constructor(
    @Named("crossfadeMainDispatcher") mainDispatcher: CoroutineDispatcher,
) {
    private var durationMs: Int = 0
    private var primary: Player? = null
    private var secondary: Player? = null
    private var wrapper: CrossfadingPlayer? = null
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private var activeRamp: Job? = null

    /**
     * Set to `true` immediately before the controller calls
     * [CrossfadingPlayer.swapTo] and reset to `false` immediately after.
     * The service's listener checks this flag to suppress the synthetic
     * `onMediaItemTransition` that [ForwardingSimpleBasePlayer.setPlayer]
     * fires, which would otherwise recurse into another crossfade.
     */
    @Volatile
    var isSwapping: Boolean = false
        private set

    /**
     * Set the secondary (inactive) player used as the mirror target.
     * Called from `FuzicPlaybackService.onCreate` after both ExoPlayers
     * are built. Exposed so JVM unit tests can inject a fake without
     * the `CrossfadingPlayer` Android-Looper requirement of [attach].
     */
    fun setSecondary(secondary: Player) {
        this.secondary = secondary
    }

    /**
     * Install the wrapper and the primary (active) ExoPlayer. Call this
     * from `FuzicPlaybackService.onCreate` once ExoPlayer construction
     * lands two players. Idempotent: re-installing replaces the previous
     * wrapper and cancels any in-flight crossfade.
     */
    fun attach(primary: Player, wrapper: CrossfadingPlayer) {
        this.primary = primary
        this.wrapper = wrapper
        activeRamp?.cancel()
    }

    /**
     * Configure the crossfade duration. 0 disables crossfade. Negative
     * values return `Result.failure`. Values > 30 000 ms (30 s) clamp to
     * 30 000 to keep the orchestrator responsive.
     */
    fun setCrossfadeDurationMs(milliseconds: Int): Result<Unit> {
        return when {
            milliseconds < 0 -> Result.failure(
                IllegalArgumentException(
                    "crossfade duration must be >= 0 (was $milliseconds)",
                ),
            )
            else -> {
                durationMs = milliseconds.coerceAtMost(MAX_CROSSFADE_MS)
                Result.success(Unit)
            }
        }
    }

    fun currentDurationMs(): Int = durationMs

    /** Cancel any in-flight crossfade ramp; used on skip / pause / stop. */
    fun cancel() {
        activeRamp?.cancel()
        activeRamp = null
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mirror methods: when crossfade is ENABLED, every transport mutation
    // from [com.androidprj.fuzic.data.repository.Media3PlayerRepository]
    // is forwarded to BOTH players via the wrapper's active player AND
    // these mirror methods on the inactive one. When crossfade is OFF
    // (durationMs == 0), each mirror method is a no-op so the existing
    // single-player behaviour is preserved without any per-call cost.
    // ────────────────────────────────────────────────────────────────────────

    /** Mirror of `Player.setMediaItems`. No-op when crossfade is disabled. */
    fun mirrorSetMediaItems(items: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        if (durationMs <= 0) return
        val inactive = secondary ?: return
        val safeIndex = startIndex.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
        inactive.setMediaItems(items, safeIndex, startPositionMs)
    }

    /** Mirror of `Player.addMediaItem`. No-op when crossfade is disabled. */
    fun mirrorAddMediaItem(item: MediaItem) {
        if (durationMs <= 0) return
        secondary?.addMediaItem(item)
    }

    /** Mirror of `Player.removeMediaItem`. No-op when crossfade is disabled. */
    fun mirrorRemoveMediaItem(index: Int) {
        if (durationMs <= 0) return
        secondary?.removeMediaItem(index)
    }

    /** Mirror of `Player.clearMediaItems`. No-op when crossfade is disabled. */
    fun mirrorClearMediaItems() {
        if (durationMs <= 0) return
        secondary?.clearMediaItems()
    }

    /** Mirror of `Player.setShuffleModeEnabled`. No-op when crossfade is disabled. */
    fun mirrorSetShuffleModeEnabled(enabled: Boolean) {
        if (durationMs <= 0) return
        secondary?.setShuffleModeEnabled(enabled)
    }

    /**
     * Mirror of `Player.setRepeatMode`. No-op when crossfade is disabled.
     * [repeatMode] is the Media3 [Player.REPEAT_MODE_OFF/_ALL/_ONE] int.
     */
    fun mirrorSetRepeatMode(repeatMode: Int) {
        if (durationMs <= 0) return
        secondary?.setRepeatMode(repeatMode)
    }

    /** Mirror of `Player.setPlaybackSpeed`. No-op when crossfade is disabled. */
    fun mirrorSetPlaybackSpeed(speed: Float) {
        if (durationMs <= 0) return
        secondary?.setPlaybackSpeed(speed)
    }

    /**
     * Hook intended for [Player.Listener.onMediaItemTransition]. When
     * the active item is about to end, prepare the next item on the
     * inactive player and ramp volumes.
     */
    fun onMediaItemTransition(
        nextMediaItem: MediaItem?,
        repeatMode: Int = Player.REPEAT_MODE_OFF,
    ) {
        if (durationMs <= 0) return
        if (nextMediaItem == null) return
        if (repeatMode == Player.REPEAT_MODE_ONE) return // no crossfade on repeat-one
        val active = primary ?: return
        val inactive = secondary ?: return
        val wrap = wrapper ?: return

        activeRamp?.cancel()
        activeRamp = scope.launch {
            inactive.setMediaItem(nextMediaItem)
            inactive.prepare()
            inactive.volume = 0f
            val position = active.currentPosition
            val total = active.duration
            val remaining = total - position
            if (remaining in 1..durationMs) {
                delay(remaining)
            }
            inactive.play()
            ramp(active, inactive, durationMs.toLong())
            // Suppress the synthetic onMediaItemTransition that
            // ForwardingSimpleBasePlayer.setPlayer emits so the service's
            // listener doesn't recurse into another crossfade attempt.
            isSwapping = true
            try {
                wrap.swapTo(inactive)
            } finally {
                isSwapping = false
            }
            // Swap references so the next call uses the new "primary"
            val oldActive = primary
            primary = secondary
            secondary = oldActive
        }
    }

    private suspend fun ramp(outgoing: Player, incoming: Player, ms: Long) {
        val outgoingRamp = VolumeRamp(ms, from = 1f, to = 0f)
        val incomingRamp = VolumeRamp(ms, from = 0f, to = 1f)
        val out = outgoingRamp.iterator()
        val `in` = incomingRamp.iterator()
        while (out.hasNext() && `in`.hasNext()) {
            outgoing.volume = out.next()
            incoming.volume = `in`.next()
            delay(VolumeRamp.DEFAULT_TICK_INTERVAL_MS)
        }
    }

    fun release() {
        cancel()
        scope.cancel()
    }

    companion object {
        const val MAX_CROSSFADE_MS: Int = 30_000
    }
}

