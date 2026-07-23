package com.androidprj.fuzic.player

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.media3.common.AudioAttributes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.androidprj.fuzic.MainActivity
import com.androidprj.fuzic.R
import com.androidprj.fuzic.player.audio.AudioProcessorRegistry
import com.androidprj.fuzic.player.cache.MediaCache
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
 *
 * ## Notification + session-activity wiring (playback-08)
 *
 * The service installs a [DefaultMediaNotificationProvider] with a stable
 * channel id and the launcher icon as the small icon, so the system media
 * notification uses Fuzic's channel instead of Media3's default. It also
 * sets a session activity on the [MediaSession] that opens [MainActivity]
 * with an `EXTRA_DESTINATION` extra (handled by a later increment); for now
 * the default Media3 behaviour — open the app and resume playback — is
 * what the user sees when tapping the notification.
 */
class FuzicPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    /**
     * Hilt entry point that surfaces [AudioProcessorRegistry] and
     * [MediaCache] to this service. Keep the interface narrow — only
     * what the service actually pulls.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PlayerEntryPoint {
        fun audioProcessorRegistry(): AudioProcessorRegistry
        fun mediaCache(): MediaCache
    }

    // Cached so onDestroy can release the SimpleCache exactly once even
    // if Media3 calls onCreate → onDestroy multiple times (e.g. during
    // task removal).
    private var mediaCacheRef: MediaCache? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val entryPoint: PlayerEntryPoint = EntryPointAccessors
            .fromApplication(applicationContext, PlayerEntryPoint::class.java)
        // Same instance Media3PlayerRepository reads visualizer frames from.
        val registry: AudioProcessorRegistry = entryPoint.audioProcessorRegistry()
        // On-disk cache for streamed playback — wired into the ExoPlayer
        // MediaSourceFactory below so seeking / replaying tracks within
        // the cache window does not hit the network.
        val mediaCache: MediaCache = entryPoint.mediaCache()
        mediaCacheRef = mediaCache

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
            // Pipe the cache through Media3's default media source factory
            // so every streamed MediaItem is read-through cached. Local
            // file:// URIs (resolved by LocalPlaybackFileResolver) bypass
            // the cache because MediaSourceFactory only intercepts http(s)
            // and content:// sources upstream of CacheDataSource.
            .setMediaSourceFactory(DefaultMediaSourceFactory(mediaCache.cacheDataSourceFactory))
            .build()
        exoPlayer = player

        // Notification provider must be installed *before* the session is
        // built so Media3's MediaNotificationManager picks it up the first
        // time a notification is posted. The channel id is stable across
        // app upgrades so user-level channel settings survive; the channel
        // name lives in strings.xml so it follows the active locale.
        // Notification provider must be installed *before* the session is
        // built so Media3's MediaNotificationManager picks it up the first
        // time a notification is posted. The channel id is stable across
        // app upgrades so user-level channel settings survive; the channel
        // name lives in strings.xml so it follows the active locale.
        // In Media3 1.10.1, setSmallIcon(int) is an instance method on the
        // provider, not on the Builder.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(FUZIC_NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.player_notification_channel_name)
            .build()
            .also { it.setSmallIcon(R.mipmap.ic_launcher) }
        setMediaNotificationProvider(notificationProvider)

        // setSessionActivity tells the system to launch MainActivity when
        // the user taps the media notification. The extra is a placeholder
        // for a future increment that routes to FullPlayerDestination;
        // for now MainActivity's default behaviour (resume the running
        // app) is what we want.
        val session = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivityPendingIntent())
            .build()
        mediaSession = session
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

    /**
     * Builds the [PendingIntent] Media3 fires when the user taps the
     * notification (or asks the system UI to open the session). The intent
     * targets [MainActivity] explicitly so we never get a manifest-filter
     * match on a future activity addition, and carries an extra the future
     * routing layer (FullPlayerDestination handling) will read.
     *
     * `FLAG_IMMUTABLE` is required on Android 12+ for any explicit
     * `ComponentName` target. `FLAG_UPDATE_CURRENT` lets the system refresh
     * the extras on the same PendingIntent if the session is rebuilt.
     * `FLAG_ACTIVITY_SINGLE_TOP` keeps the existing MainActivity instance
     * alive (no duplicate back-stack entry), and `FLAG_ACTIVITY_CLEAR_TOP`
     * evicts any obsolete destinations above it.
     */
    private fun buildSessionActivityPendingIntent(): PendingIntent {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(this@FuzicPlaybackService, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Placeholder extra — handled by a later increment. Media3's
            // default behaviour (open the app) is what users see today.
            putExtra(EXTRA_DESTINATION, DESTINATION_FULL_PLAYER)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(
            this,
            /* requestCode = */ 0,
            launchIntent,
            flags,
        )
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        // Release the SQLite-backed cache after Media3 has stopped reading
        // from it. runBlocking is acceptable here — MediaSessionService
        // teardown is short-lived and the cache close is a single SQLite
        // close() call.
        mediaCacheRef?.let { cache ->
            try {
                kotlinx.coroutines.runBlocking { cache.release() }
            } finally {
                mediaCacheRef = null
            }
        }
        super.onDestroy()
    }

    private companion object {
        const val FUZIC_NOTIFICATION_CHANNEL_ID = "fuzic.media.playback"
        const val EXTRA_DESTINATION = "fuzic.extra.destination"
        const val DESTINATION_FULL_PLAYER = "full_player"
    }
}
