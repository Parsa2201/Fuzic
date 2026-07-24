package com.androidprj.fuzic.data.repository

import androidx.media3.common.Player
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Small ticker that refreshes `PlayerUiState.progress`,
 * `elapsedLabel` and `durationLabel` on a fixed cadence while
 * playback is active.
 *
 * ## Why a dedicated class?
 *
 * The polling loop, the "mm:ss" / "h:mm:ss" formatter, and the
 * 250 ms cadence constant were inlined into the repository in
 * playback-05 but pushed it past the 350-line soft cap. Extracting
 * keeps the repository focused on transport commands and put the
 * pure-progress-mirror logic in a place that can later be unit
 * tested on the JVM.
 *
 * ## Lifecycle
 *
 * The repository owns one instance and starts it inside its `init`
 * block. Restarting is supported via [start] which cancels the prior
 * [job] first. Stopping forever is supported via [stop].
 *
 * ## Threading
 *
 * The ticker hops to [Dispatchers.Main.immediate] because it reads
 * [androidx.media3.common.Player.currentPosition] / `duration` /
 * `playbackState`. Writes flow through the supplied
 * [playerState] (already a [MutableStateFlow]) which is safe to
 * update from any thread.
 */
internal class Media3ProgressPoller(
    private val playerController: PlayerController,
    private val playerState: MutableStateFlow<PlayerUiState>,
) {

    private var job: Job? = null

    /**
     * (Re)launches the polling loop on [scope]. Idempotent: calling
     * twice cancels the prior loop. Always uses 250 ms cadence.
     */
    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch { runLoop() }
    }

    /** Cancels the active loop, if any. Safe to call when no loop
     * has been [start]ed. */
    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun runLoop() = withContext(Dispatchers.Main.immediate) {
        val player = playerController.controller()
        while (coroutineContext[Job]?.isActive == true) {
            val position = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration
            // Treat C.TIME_UNSET (== Long.MIN_VALUE) and any
            // non-positive duration as "no progress yet". Avoids
            // dividing by zero / negative numbers.
            if (duration > 0L) {
                val safeDuration = duration.coerceAtLeast(1L)
                val progress = (position.toFloat() / safeDuration.toFloat())
                    .coerceIn(0f, 1f)
                playerState.update {
                    it.copy(
                        progress = progress,
                        elapsedLabel = formatMillis(position),
                        durationLabel = formatMillis(duration),
                    )
                }
            }
            // We keep polling even if STATE_ENDED, so the UI can pick up
            // the new song when playback resumes or a new item is selected.
            delay(PROGRESS_POLL_INTERVAL_MS)
        }
    }

    private companion object {
        const val PROGRESS_POLL_INTERVAL_MS: Long = 250L
    }
}

/** Formats a duration in milliseconds as "m:ss" or "h:mm:ss" when
 * the value crosses the one-hour mark. Negative inputs are clamped
 * to zero. Pure — easy to unit test in plain JVM. */
internal fun formatMillis(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
