package com.androidprj.fuzic.player

import androidx.annotation.VisibleForTesting
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class FuzicPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
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