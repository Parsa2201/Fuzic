package com.androidprj.fuzic.player

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.media3.common.AudioAttributes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.androidprj.fuzic.player.audio.AudioProcessorRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background [MediaSessionService] that hosts ExoPlayer and exposes it to
 * the rest of the app via a [MediaSession].
 *
 * ## Visualizer wiring
 *
 * The service looks up the singleton [AudioProcessorRegistry] through
 * [PlayerEntryPoint] (Hilt's entry-point accessor pattern, used because
 * `MediaSessionService` is not Hilt-injectable the way Activities are). The
 * registry exposes the same `AmplitudeAudioProcessor` instance that
 * `Media3PlayerRepository.visualizerFrames` reads frames from — without
 * this, the audio thread would push frames that no consumer observes.
 *
 * The processor is installed into the audio chain by overriding
 * [DefaultRenderersFactory.buildAudioSink]: Media3 1.10.1 marks that method
 * `@ForOverride` so it is the supported extension point for the audio sink.
 */
class FuzicPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    /**
     * Hilt entry point that surfaces [AudioProcessorRegistry] to this
     * service. Keep the interface narrow — only what the service actually
     * pulls.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PlayerEntryPoint {
        fun audioProcessorRegistry(): AudioProcessorRegistry
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Same instance Media3PlayerRepository reads visualizer frames from.
        val registry: AudioProcessorRegistry = EntryPointAccessors
            .fromApplication(applicationContext, PlayerEntryPoint::class.java)
            .audioProcessorRegistry()

        // Build the audio sink with the visualizer processor in its chain.
        // Media3's DefaultAudioSink installs silence-skipping and Sonic
        // speed processors alongside whatever we add here, so playback
        // stays normal even with our processor present.
        val audioSink: AudioSink = DefaultAudioSink.Builder(this)
            .setAudioProcessors(arrayOf<AudioProcessor>(registry.processor))
            .build()

        // Override DefaultRenderersFactory.buildAudioSink so ExoPlayer
        // receives our pre-built sink. The default path constructs its
        // own sink without our processor — this is the official 1.10.1
        // extension point for the audio sink.
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean,
            ): AudioSink = audioSink
        }

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setRenderersFactory(renderersFactory)
            .build()
        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Token for the currently active [MediaSession], or `null` before
     * [onCreate] has run or after [onDestroy] has cleared it.
     */
    @UnstableApi
    fun sessionToken(): SessionToken? = mediaSession?.token

    /**
     * Test-only accessor that exposes the underlying [ExoPlayer]. Never call
     * this from production code; use [PlayerController] or `MediaController`
     * instead.
     */
    @VisibleForTesting
    fun currentExoPlayer(): ExoPlayer? = exoPlayer

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}
