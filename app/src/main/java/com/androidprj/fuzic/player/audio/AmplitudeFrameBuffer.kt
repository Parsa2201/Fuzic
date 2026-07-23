package com.androidprj.fuzic.player.audio

import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Thread-safe bridge between the audio thread (which feeds [push]) and the
 * UI's collector (which subscribes to [flow]).
 *
 * The channel uses `capacity = 2` and [BufferOverflow.DROP_OLDEST] so a slow
 * visualizer consumer cannot stall the audio processor: at ~30 Hz a 2-slot
 * buffer provides roughly one frame of headroom and silently drops anything
 * older when the UI lags. Visualizer frames are inherently lossy snapshots
 * — losing a stale one is preferable to audio underruns.
 *
 * ## Unicast
 *
 * The exposed [Flow] is backed by [Channel.receiveAsFlow], which fans out to
 * at most a single collector. The repository's visualizer flow is the only
 * intended subscriber, and the UI uses `flatMapLatest` to cancel any prior
 * collection before starting a new one, so unicast semantics line up with
 * the actual call sites.
 */
class AmplitudeFrameBuffer {

    private val channel: Channel<AudioVisualizerFrame> = Channel(
        capacity = CHANNEL_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Hands the most recent amplitude frame to consumers. Non-blocking — uses
     * [Channel.trySend] which silently drops when the buffer is full
     * ([DROP_OLDEST]). Safe to call from the audio processor thread.
     *
     * Allocates a defensive copy of [amplitudes] so callers may reuse the
     * ring buffer the array originated from on the next FFT cycle.
     */
    fun push(amplitudes: FloatArray) {
        channel.trySend(
            AudioVisualizerFrame(
                amplitudes = amplitudes.toList(),
                timestampEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Cold [Flow] over the channel's emissions. Each call returns a fresh
     * flow that observes subsequent `push` events from the moment it is
     * collected.
     */
    fun flow(): Flow<AudioVisualizerFrame> = channel.receiveAsFlow()

    private companion object {
        /** Two slots: one in flight + one queued. Older frames drop on overrun. */
        const val CHANNEL_CAPACITY: Int = 2
    }
}
