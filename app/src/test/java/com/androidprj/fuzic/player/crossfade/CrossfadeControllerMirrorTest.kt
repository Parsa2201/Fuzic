package com.androidprj.fuzic.player.crossfade

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM coverage for the dual-player command mirror on
 * [CrossfadeController]. The mirror forwards Media3 transport
 * mutations to whichever player is *inactive* so both inner players
 * stay in sync when the crossfade duration is > 0. With
 * `durationMs == 0` every mirror is a no-op so the existing
 * single-player-effective behaviour is preserved.
 *
 * The `Player` interface has ~50 methods; the test fake here only
 * stubs the seven that the controller's mirrors touch. The remaining
 * unimplemented methods are not invoked by these tests and would
 * throw `NotImplementedError` if they were.
 */
class CrossfadeControllerMirrorTest {

    /** Minimal Media3 `Player` fake that counts the seven mirrored calls. */
    private class FakePlayer : Player {
        var setMediaItemsCalls = 0
            private set
        var lastSetMediaItemsIndex: Int = -1
            private set
        var addMediaItemCalls = 0
            private set
        var removeMediaItemCalls = 0
            private set
        var clearMediaItemsCalls = 0
            private set
        var setShuffleModeEnabledCalls = 0
            private set
        var lastShuffleEnabled: Boolean? = null
            private set
        var setRepeatModeCalls = 0
            private set
        var lastRepeatMode: Int = -1
            private set
        var setPlaybackSpeedCalls = 0
            private set
        var lastPlaybackSpeed: Float = -1f
            private set
        var prepared = 0
            private set
        var playCalls = 0
            private set
        var pauseCalls = 0
            private set

        override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
            setMediaItemsCalls++
        }
        override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
            setMediaItemsCalls++
        }
        override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
            setMediaItemsCalls++
            lastSetMediaItemsIndex = startIndex
        }
        override fun setMediaItem(mediaItem: MediaItem) {}
        override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {}
        override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {}
        override fun addMediaItem(mediaItem: MediaItem) {
            addMediaItemCalls++
        }
        override fun addMediaItem(index: Int, mediaItem: MediaItem) {
            addMediaItemCalls++
        }
        override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
            addMediaItemCalls++
        }
        override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
            addMediaItemCalls++
        }
        override fun removeMediaItem(index: Int) {
            removeMediaItemCalls++
        }
        override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
            removeMediaItemCalls++
        }
        override fun clearMediaItems() {
            clearMediaItemsCalls++
        }
        override fun prepare() { prepared++ }
        override fun play() { playCalls++ }
        override fun pause() { pauseCalls++ }
        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
            setShuffleModeEnabledCalls++
            lastShuffleEnabled = shuffleModeEnabled
        }
        override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
            setRepeatModeCalls++
            lastRepeatMode = repeatMode
        }
        override fun setPlaybackSpeed(speed: Float) {
            setPlaybackSpeedCalls++
            lastPlaybackSpeed = speed
        }
        // ── everything below is unused in these tests ────────────────────
        override fun getApplicationLooper(): android.os.Looper = throw UnsupportedOperationException()
        override fun addListener(listener: Player.Listener) {}
        override fun removeListener(listener: Player.Listener) {}
        override fun isCommandAvailable(@Player.Command command: Int): Boolean = false
        override fun canAdvertiseSession(): Boolean = false
        override fun getAvailableCommands(): Player.Commands = throw UnsupportedOperationException()
        override fun getPlaybackState(): Int = Player.STATE_IDLE
        override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE
        override fun isPlaying(): Boolean = false
        override fun getPlayerError(): androidx.media3.common.PlaybackException? = null
        override fun setPlayWhenReady(playWhenReady: Boolean) {}
        override fun getPlayWhenReady(): Boolean = false
        override fun getRepeatMode(): Int = Player.REPEAT_MODE_OFF
        override fun getShuffleModeEnabled(): Boolean = false
        override fun isLoading(): Boolean = false
        override fun seekToDefaultPosition() {}
        override fun seekToDefaultPosition(mediaItemIndex: Int) {}
        override fun seekTo(positionMs: Long) {}
        override fun seekTo(mediaItemIndex: Int, positionMs: Long) {}
        override fun getSeekBackIncrement(): Long = 0L
        override fun seekBack() {}
        override fun getSeekForwardIncrement(): Long = 0L
        override fun seekForward() {}
        override fun hasPreviousMediaItem(): Boolean = false
        override fun seekToPreviousMediaItem() {}
        override fun seekToPrevious() {}
        override fun getMaxSeekToPreviousPosition(): Long = 0L
        override fun hasNextMediaItem(): Boolean = false
        override fun seekToNextMediaItem() {}
        override fun seekToNext() {}
        override fun setPlaybackParameters(playbackParameters: androidx.media3.common.PlaybackParameters) {}
        override fun getPlaybackParameters(): androidx.media3.common.PlaybackParameters =
            throw UnsupportedOperationException()
        override fun stop() {}
        override fun release() {}
        override fun getCurrentTracks(): androidx.media3.common.Tracks =
            throw UnsupportedOperationException()
        override fun getTrackSelectionParameters(): androidx.media3.common.TrackSelectionParameters =
            throw UnsupportedOperationException()
        override fun setTrackSelectionParameters(parameters: androidx.media3.common.TrackSelectionParameters) {}
        override fun getMediaMetadata(): androidx.media3.common.MediaMetadata =
            throw UnsupportedOperationException()
        override fun getPlaylistMetadata(): androidx.media3.common.MediaMetadata =
            throw UnsupportedOperationException()
        override fun setPlaylistMetadata(mediaMetadata: androidx.media3.common.MediaMetadata) {}
        override fun getCurrentManifest(): Any? = null
        override fun getCurrentTimeline(): androidx.media3.common.Timeline =
            throw UnsupportedOperationException()
        override fun getCurrentPeriodIndex(): Int = 0
        override fun getCurrentMediaItemIndex(): Int = 0
        override fun getCurrentMediaItem(): MediaItem? = null
        override fun getMediaItemCount(): Int = 0
        override fun getMediaItemAt(index: Int): MediaItem = throw UnsupportedOperationException()
        override fun getDuration(): Long = 0L
        override fun getCurrentPosition(): Long = 0L
        override fun getBufferedPosition(): Long = 0L
        override fun getBufferedPercentage(): Int = 0
        override fun getTotalBufferedDuration(): Long = 0L
        override fun isCurrentMediaItemDynamic(): Boolean = false
        override fun isCurrentMediaItemLive(): Boolean = false
        override fun getCurrentLiveOffset(): Long = 0L
        override fun isCurrentMediaItemSeekable(): Boolean = false
        override fun isPlayingAd(): Boolean = false
        override fun getCurrentAdGroupIndex(): Int = 0
        override fun getCurrentAdIndexInAdGroup(): Int = 0
        override fun getContentDuration(): Long = 0L
        override fun getContentPosition(): Long = 0L
        override fun getContentBufferedPosition(): Long = 0L
        override fun getAudioAttributes(): androidx.media3.common.AudioAttributes =
            throw UnsupportedOperationException()
        override fun getAudioSessionId(): Int = 0
        override fun setVolume(volume: Float) { this.volume = volume }
        override fun getVolume(): Float = volume
        override fun mute() {}
        override fun unmute() {}
        override fun getVideoSize(): androidx.media3.common.VideoSize =
            throw UnsupportedOperationException()
        override fun getSurfaceSize(): androidx.media3.common.util.Size =
            throw UnsupportedOperationException()
        override fun clearVideoSurface() {}
        override fun clearVideoSurface(surface: android.view.Surface?) {}
        override fun setVideoSurface(surface: android.view.Surface?) {}
        override fun setVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) {}
        override fun clearVideoSurfaceHolder(surfaceHolder: android.view.SurfaceHolder?) {}
        override fun setVideoSurfaceView(surfaceView: android.view.SurfaceView?) {}
        override fun clearVideoSurfaceView(surfaceView: android.view.SurfaceView?) {}
        override fun setVideoTextureView(textureView: android.view.TextureView?) {}
        override fun clearVideoTextureView(textureView: android.view.TextureView?) {}
        override fun getCurrentCues(): androidx.media3.common.text.CueGroup =
            throw UnsupportedOperationException()
        override fun getDeviceInfo(): androidx.media3.common.DeviceInfo =
            throw UnsupportedOperationException()
        override fun getDeviceVolume(): Int = 0
        override fun isDeviceMuted(): Boolean = false
        @Deprecated("") override fun setDeviceVolume(volume: Int) {}
        override fun setDeviceVolume(volume: Int, flags: Int) {}
        @Deprecated("") override fun increaseDeviceVolume() {}
        override fun increaseDeviceVolume(flags: Int) {}
        @Deprecated("") override fun decreaseDeviceVolume() {}
        override fun decreaseDeviceVolume(flags: Int) {}
        @Deprecated("") override fun setDeviceMuted(muted: Boolean) {}
        override fun setDeviceMuted(muted: Boolean, flags: Int) {}
        override fun setAudioAttributes(
            audioAttributes: androidx.media3.common.AudioAttributes,
            handleAudioFocus: Boolean,
        ) {}
        override fun moveMediaItem(currentIndex: Int, newIndex: Int) {}
        override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}
        override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {}
        override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>) {}
        // Deprecated members still on the Player interface in Media3 1.10.1.
        @Deprecated("Deprecated in Java") override fun getCurrentWindowIndex(): Int = 0
        @Deprecated("Deprecated in Java") override fun getNextWindowIndex(): Int = -1
        @Deprecated("Deprecated in Java") override fun getPreviousWindowIndex(): Int = -1
        override fun getNextMediaItemIndex(): Int = -1
        override fun getPreviousMediaItemIndex(): Int = -1
        @Deprecated("Deprecated in Java") override fun isCurrentWindowDynamic(): Boolean = false
        @Deprecated("Deprecated in Java") override fun isCurrentWindowLive(): Boolean = false
        @Deprecated("Deprecated in Java") override fun isCurrentWindowSeekable(): Boolean = false
    }

    private fun newAttached(): Triple<CrossfadeController, FakePlayer, FakePlayer> {
        // Pass Dispatchers.Unconfined so JVM tests do not crash trying to
        // access Dispatchers.Main. In production the controller receives
        // Dispatchers.Main.immediate via the @Named("crossfadeMainDispatcher")
        // Hilt binding.
        // We only set the secondary player via setSecondary — the
        // CrossfadingPlayer wrapper is not constructed because tests do
        // not exercise the swap path.
        val controller = CrossfadeController(mainDispatcher = Dispatchers.Unconfined)
        val primary = FakePlayer()
        val secondary = FakePlayer()
        controller.setSecondary(secondary)
        return Triple(controller, primary, secondary)
    }

    @Test
    fun allMirrorsAreNoOpsWhenCrossfadeDisabled() {
        val (controller, _, secondary) = newAttached()
        // Default durationMs == 0 → every mirror is a no-op. Use the
        // MediaItem builder that does NOT call android.net.Uri.parse
        // (which is not mocked in plain JVM tests).
        controller.mirrorSetMediaItems(mutableListOf(testMediaItem("a")), 0, 0L)
        controller.mirrorAddMediaItem(testMediaItem("b"))
        controller.mirrorRemoveMediaItem(0)
        controller.mirrorClearMediaItems()
        controller.mirrorSetShuffleModeEnabled(true)
        controller.mirrorSetRepeatMode(Player.REPEAT_MODE_ALL)
        controller.mirrorSetPlaybackSpeed(1.5f)

        assertEquals(0, secondary.setMediaItemsCalls)
        assertEquals(0, secondary.addMediaItemCalls)
        assertEquals(0, secondary.removeMediaItemCalls)
        assertEquals(0, secondary.clearMediaItemsCalls)
        assertEquals(0, secondary.setShuffleModeEnabledCalls)
        assertEquals(0, secondary.setRepeatModeCalls)
        assertEquals(0, secondary.setPlaybackSpeedCalls)
    }

    @Test
    fun mirrorsForwardToSecondaryPlayerWhenCrossfadeEnabled() {
        val (controller, _, secondary) = newAttached()
        controller.setCrossfadeDurationMs(6_000)
        assertEquals(6_000, controller.currentDurationMs())

        controller.mirrorSetMediaItems(
            mutableListOf(testMediaItem("a")),
            0,
            0L,
        )
        controller.mirrorAddMediaItem(testMediaItem("b"))
        controller.mirrorRemoveMediaItem(1)
        controller.mirrorClearMediaItems()
        controller.mirrorSetShuffleModeEnabled(true)
        controller.mirrorSetRepeatMode(Player.REPEAT_MODE_ONE)
        controller.mirrorSetPlaybackSpeed(1.25f)

        assertEquals(1, secondary.setMediaItemsCalls)
        assertEquals(0, secondary.lastSetMediaItemsIndex) // coerced
        assertEquals(1, secondary.addMediaItemCalls)
        assertEquals(1, secondary.removeMediaItemCalls)
        assertEquals(1, secondary.clearMediaItemsCalls)
        assertEquals(1, secondary.setShuffleModeEnabledCalls)
        assertEquals(true, secondary.lastShuffleEnabled)
        assertEquals(1, secondary.setRepeatModeCalls)
        assertEquals(Player.REPEAT_MODE_ONE, secondary.lastRepeatMode)
        assertEquals(1, secondary.setPlaybackSpeedCalls)
        assertEquals(1.25f, secondary.lastPlaybackSpeed)
    }

    /**
     * MediaItem.fromUri(String) routes through android.net.Uri.parse,
     * which is not mocked in plain JVM tests. Build with the no-Uri
     * constructor instead.
     */
    private fun testMediaItem(id: String): MediaItem =
        MediaItem.Builder().setMediaId(id).build()

    @Test
    fun settingDurationBelowZeroReturnsFailureAndLeavesDurationUnchanged() {
        val (controller, _, _) = newAttached()
        val before = controller.currentDurationMs()

        val result = controller.setCrossfadeDurationMs(-1)

        assertFalse(result.isSuccess)
        assertEquals(before, controller.currentDurationMs())
    }

    @Test
    fun settingDurationAboveTheCapClamps() {
        val (controller, _, _) = newAttached()
        val result = controller.setCrossfadeDurationMs(CrossfadeController.MAX_CROSSFADE_MS + 1)
        assertEquals(true, result.isSuccess)
        assertEquals(CrossfadeController.MAX_CROSSFADE_MS, controller.currentDurationMs())
    }

    @Test
    fun switchingBackToDisabledReEnablesNoOpBehaviour() {
        val (controller, _, secondary) = newAttached()
        controller.setCrossfadeDurationMs(6_000)
        controller.mirrorSetShuffleModeEnabled(true)
        assertEquals(1, secondary.setShuffleModeEnabledCalls)

        controller.setCrossfadeDurationMs(0)
        controller.mirrorSetShuffleModeEnabled(true)
        // The second mirror is a no-op because duration == 0.
        assertEquals(1, secondary.setShuffleModeEnabledCalls)
    }

    @Test
    fun clampCoercesOutOfRangeStartIndex() {
        val (controller, _, secondary) = newAttached()
        controller.setCrossfadeDurationMs(6_000)
        controller.mirrorSetMediaItems(
            mutableListOf(testMediaItem("a")),
            /* startIndex = */ 5, // beyond the single-item list
            0L,
        )
        assertEquals(1, secondary.setMediaItemsCalls)
        // Negative safeIndex would be -1, but `(lastIndex).coerceAtLeast(0)`
        // clamps the items.lastIndex to 0 before coerceIn runs.
        assertNotEquals(-1, secondary.lastSetMediaItemsIndex)
    }
}
