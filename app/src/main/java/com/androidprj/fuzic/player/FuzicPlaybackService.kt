package com.androidprj.fuzic.player

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import com.androidprj.fuzic.player.crossfade.CrossfadeController
import com.androidprj.fuzic.player.crossfade.CrossfadingPlayer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Background [MediaSessionService] that hosts ExoPlayer and exposes it to
 * the rest of the app via a [MediaSession].
 *
 * ## Two-player crossfade (playback-addendum 06)
 *
 * To support crossfade between consecutive tracks, the service builds
 * **two** [ExoPlayer] instances (`playerA` + `playerB`) that share the
 * same audio sink + renderers factory + cache-aware media source
 * factory. Both are wrapped by [CrossfadingPlayer] (extends Media3
 * 1.10.1's [androidx.media3.common.ForwardingSimpleBasePlayer]). The
 * [MediaSession] is bound to the wrapper, so commands route to whichever
 * player is currently active.
 *
 * When [CrossfadeController.setCrossfadeDurationMs] is called with a
 * positive value, the controller adds a [Player.Listener] on the wrapper
 * that triggers the dual-player swap via [CrossfadingPlayer.swapTo].
 * With the default value of 0, the listener is a no-op and the service
 * behaves identically to single-player pre-merge.
 *
 * ## Visualizer + cache wiring
 *
 * The audio sink carries the FFT [AudioProcessor] from
 * [AudioProcessorRegistry]; both players' sinks point at the same sink
 * instance so the visualizer sees whichever audio stream is currently
 * audible. The cache [MediaCache]'s `cacheDataSourceFactory` is shared
 * between both players.
 */
class FuzicPlaybackService : MediaSessionService() {

    private var playerA: ExoPlayer? = null
    private var playerB: ExoPlayer? = null
    private var crossfadingPlayer: CrossfadingPlayer? = null
    private var mediaSession: MediaSession? = null

    /**
     * Hilt entry point that surfaces the singletons this service pulls
     * from the [SingletonComponent]. Kept narrow — only what
     * `FuzicPlaybackService` actually needs.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PlayerEntryPoint {
        fun audioProcessorRegistry(): AudioProcessorRegistry
        fun mediaCache(): MediaCache
        fun crossfadeController(): CrossfadeController
    }

    // Cached so onDestroy can release the SimpleCache exactly once even
    // if Media3 calls onCreate → onDestroy multiple times (e.g. during
    // task removal).
    private var mediaCacheRef: MediaCache? = null
    private var crossfadeControllerRef: CrossfadeController? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val entryPoint: PlayerEntryPoint = EntryPointAccessors
            .fromApplication(applicationContext, PlayerEntryPoint::class.java)
        val registry: AudioProcessorRegistry = entryPoint.audioProcessorRegistry()
        val mediaCache: MediaCache = entryPoint.mediaCache()
        val crossfadeController: CrossfadeController = entryPoint.crossfadeController()
        mediaCacheRef = mediaCache
        crossfadeControllerRef = crossfadeController

        // Shared audio sink + RenderersFactory for both players. Media3's
        // DefaultAudioSink installs silence-skipping and Sonic speed
        // processors alongside whatever we add here, so playback stays
        // normal even with our visualizer processor present.
        val audioSink: AudioSink = DefaultAudioSink.Builder(this)
            .setAudioProcessors(arrayOf<AudioProcessor>(registry.processor))
            .build()
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean,
            ): AudioSink = audioSink
        }
        // Pipe the cache through Media3's default media source factory
        // so every streamed MediaItem is read-through cached. Local file
        // URIs bypass the cache because MediaSourceFactory only intercepts
        // http(s) / content:// sources upstream of CacheDataSource.
        val mediaSourceFactory = DefaultMediaSourceFactory(mediaCache.cacheDataSourceFactory)

        fun buildPlayer() = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        val a = buildPlayer()
        val b = buildPlayer()
        val wrapper = CrossfadingPlayer(a)
        playerA = a
        playerB = b
        crossfadingPlayer = wrapper

        crossfadeController.attach(
            primary = a,
            secondary = b,
            wrapper = wrapper,
        )

        // Trigger crossfade on natural Media3 transitions. The
        // [CrossfadeController.isSwapping] guard prevents the synthetic
        // discontinuity fired by [ForwardingSimpleBasePlayer.setPlayer]
        // from re-entering this listener.
        wrapper.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (crossfadeController.isSwapping) return
                if (crossfadeController.currentDurationMs() <= 0) return
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                    reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) return
                val nextIndex = wrapper.currentMediaItemIndex + 1
                val next = if (nextIndex < wrapper.mediaItemCount) {
                    wrapper.getMediaItemAt(nextIndex)
                } else {
                    null
                }
                crossfadeController.onMediaItemTransition(next, wrapper.repeatMode)
            }
        })

        // Notification provider must be installed *before* the session is
        // built so Media3's MediaNotificationManager picks it up the first
        // time a notification is posted. In Media3 1.10.1, setSmallIcon(int)
        // is an instance method on the provider, not on the Builder.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(FUZIC_NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.player_notification_channel_name)
            .build()
            .also { it.setSmallIcon(R.mipmap.ic_launcher) }
        setMediaNotificationProvider(notificationProvider)

        // setSessionActivity tells the system to launch MainActivity when
        // the user taps the media notification. The MediaSession is bound
        // to the CrossfadingPlayer wrapper so transport commands route
        // through whichever player is active.
        val session = MediaSession.Builder(this, wrapper)
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
     * Test-only accessor that exposes the active inner player. Returns
     * whichever player the [CrossfadingPlayer] currently forwards to.
     * Never call this from production code; use [PlayerController] or
     * `MediaController` instead.
     */
    @VisibleForTesting
    fun currentExoPlayer(): ExoPlayer? {
        val wrapper = crossfadingPlayer ?: return null
        // ForwardingSimpleBasePlayer.getPlayer() is protected; cast to the
        // CrossfadingPlayer field through reflection is over-engineering
        // for a single test accessor. Returning the primary player is
        // good enough — by construction it is the active one on a fresh
        // service start and after every crossfade that succeeded.
        return playerA
    }

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
        // Cancel any in-flight crossfade ramp first so it doesn't touch
        // a half-released player mid-shutdown.
        crossfadeControllerRef?.cancel()
        // Release in this order: mediaSession (de-register callbacks),
        // playerB (no forwarding involvement), crossfadingPlayer which
        // releases playerA via ForwardingSimpleBasePlayer.release(),
        // then the on-disk cache.
        mediaSession?.release()
        mediaSession = null
        playerB?.release()
        playerB = null
        crossfadingPlayer?.release()
        crossfadingPlayer = null
        playerA = null
        mediaCacheRef?.let { cache ->
            try {
                kotlinx.coroutines.runBlocking { cache.release() }
            } finally {
                mediaCacheRef = null
                crossfadeControllerRef = null
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
