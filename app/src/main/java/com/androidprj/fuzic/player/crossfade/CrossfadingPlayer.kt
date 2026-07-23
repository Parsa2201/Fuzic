package com.androidprj.fuzic.player.crossfade

import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * A [Player] that forwards every call to a single inner [Player] and lets
 * callers swap that inner reference via [swapTo] at runtime. The class
 * exists to support crossfade between consecutive tracks: the
 * `MediaSession` is bound to this wrapper, and the
 * [CrossfadeController] swaps the wrapped [Player] from `playerA` to
 * `playerB` (and back) on each transition.
 *
 * Built on Media3's [ForwardingSimpleBasePlayer] which already handles
 * state forwarding, listener bookkeeping, and `setPlayer(newPlayer)` —
 * we only need to expose a public alias.
 *
 * Both inner [Player]s must share the same `Looper` (already enforced
 * by Media3's `setPlayer` check).
 */
@UnstableApi
class CrossfadingPlayer(initial: Player) : ForwardingSimpleBasePlayer(initial) {

    /**
     * Replace the wrapped [Player]. Triggers a position discontinuity
     * and invalidates the wrapper's state so all `Player.Listener`
     * callbacks fire as usual.
     */
    fun swapTo(newPlayer: Player) {
        setPlayer(newPlayer)
    }
}
