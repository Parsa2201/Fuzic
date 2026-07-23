package com.androidprj.fuzic.player.audio

import androidx.media3.common.util.UnstableApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-scoped singleton that owns both the visualizer's [AmplitudeFrameBuffer]
 * and the [AmplitudeAudioProcessor] that feeds it.
 *
 * ## Why a registry?
 *
 * Two callers need the same instances:
 *
 * - `Media3PlayerRepository` is `@Inject constructor` and exposes the
 *   visualizer flow through `visualizerFrames`.
 * - `FuzicPlaybackService` is not Hilt-aware (`MediaSessionService` is not
 *   annotated `@AndroidEntryPoint`), so it looks the registry up via
 *   [dagger.hilt.android.EntryPointAccessors] to install the processor into
 *   ExoPlayer's audio chain.
 *
 * If each side constructed its own processor, the audio thread would push
 * into the repository's nowhere-land and the visualizer would render a frozen
 * frame. Holding both objects in one `@Singleton` keeps them in lock-step.
 */
@Singleton
class AudioProcessorRegistry @Inject constructor() {

    /**
     * The channel-backed frame bridge. Public so [Media3PlayerRepository] can
     * expose it as a flow and so tests can drain it without going through
     * the processor.
     */
    val frameBuffer: AmplitudeFrameBuffer = AmplitudeFrameBuffer()

    /**
     * The single [AmplitudeAudioProcessor] wired into ExoPlayer. Public so
     * `FuzicPlaybackService` can hand it to `DefaultAudioSink.Builder`. The
     * [UnstableApi] opt-in matches the underlying Media3 type that
     * [AmplitudeAudioProcessor] inherits from.
     */
    @get:UnstableApi
    val processor: AmplitudeAudioProcessor = AmplitudeAudioProcessor(frameBuffer)
}
