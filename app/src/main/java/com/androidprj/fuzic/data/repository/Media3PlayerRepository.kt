package com.androidprj.fuzic.data.repository

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.player.PlayerController
import com.androidprj.fuzic.repository.PlayerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Media3-backed [PlayerRepository] owning the single [PlayerUiState] snapshot
 * for the whole app. Every transport call hops to the injected IO dispatcher
 * for cooperative cancellation and then to [Dispatchers.Main] because Media3's
 * controller API is main-thread only. Queue (playback-04), sleep timer
 * (playback-05), and visualizer frames (playback-06) intentionally return
 * controlled failures today.
 */
@Singleton
class Media3PlayerRepository @Inject constructor(
    private val playerController: PlayerController,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlayerRepository {

    // Exposed as MutableStateFlow so the listener can update the snapshot
    // directly; external callers see only StateFlow via the interface.
    override val playerState: MutableStateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState())

    override val visualizerFrames: Flow<AudioVisualizerFrame> = emptyFlow()

    // Authoritative SongItem mirror. MediaItem carries no app metadata, so
    // play() stores the originating SongItem here for the listener to recover.
    private val currentSongMirror: MutableStateFlow<SongItem?> = MutableStateFlow(null)

    // Process-scoped listener scope — survives every ViewModel and screen
    // rotation so playback callbacks keep updating state during navigation.
    @OptIn(DelicateCoroutinesApi::class)
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val listener: Player.Listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            playerState.update { state ->
                when (playbackState) {
                    Player.STATE_BUFFERING -> state.copy(isBuffering = true)
                    Player.STATE_READY -> state.copy(isBuffering = false)
                    else -> state
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // MediaItem is nullable on queue-end transitions; toSongItemOrNull
            // surfaces null in that case so the mini-player doesn't go stale.
            playerState.update { it.copy(currentSong = toSongItemOrNull(mediaItem)) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            playerState.update { it.copy(repeatMode = toRepeatMode(repeatMode)) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            playerState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            playerState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onPlayerError(error: PlaybackException) {
            // No rethrow — surface a localized message into the UI state and
            // let the playback continue if ExoPlayer decides to retry.
            playerState.update {
                it.copy(
                    errorMessage = error.localizedMessage
                        ?: error.message
                        ?: "Unknown playback error",
                )
            }
        }
    }

    // Placeholder progress-polling job. playback-05 replaces this with a
    // real ticker; today PlayerUiState defaults supply the labels.
    private val progressPollingJob: Job = scope.launch { /* no-op until playback-05 */ }

    init {
        scope.launch { playerController.addListener(listener) }
    }

    override suspend fun play(song: SongItem): Result<Unit> = runOnMain {
        val url = song.audioUrl
        if (url.isNullOrEmpty()) {
            return@runOnMain Result.failure(IllegalArgumentException("song has no audioUrl"))
        }
        val controller = playerController.controller()
        // Stash the SongItem before issuing transport commands so the
        // subsequent onMediaItemTransition callback can recover it.
        currentSongMirror.value = song
        controller.setMediaItem(MediaItem.fromUri(url), 0L)
        controller.prepare()
        controller.play()
        Result.success(Unit)
    }

    override suspend fun playQueue(
        songs: List<SongItem>,
        startIndex: Int,
    ): Result<Unit> =
        Result.failure(UnsupportedOperationException("queue integration lands in playback-04"))

    override suspend fun togglePlayPause(): Result<Unit> = runOnMain {
        val controller = playerController.controller()
        if (controller.isPlaying) controller.pause() else controller.play()
        Result.success(Unit)
    }

    override suspend fun skipToNext(): Result<Unit> = runOnMain {
        playerController.controller().seekToNextMediaItem()
        Result.success(Unit)
    }

    override suspend fun skipToPrevious(): Result<Unit> = runOnMain {
        playerController.controller().seekToPreviousMediaItem()
        Result.success(Unit)
    }

    override suspend fun seekTo(progress: Float): Result<Unit> = runOnMain {
        val controller = playerController.controller()
        val duration = controller.duration
        if (duration == C.TIME_UNSET) {
            // Pre-preparation Media3 reports TIME_UNSET; surface it as a
            // controlled failure instead of silently seeking to 0.
            return@runOnMain Result.failure(
                IllegalStateException("playback duration is not ready"),
            )
        }
        val positionMs = (progress.coerceIn(0f, 1f) * duration).toLong()
        controller.seekTo(controller.currentMediaItemIndex, positionMs)
        Result.success(Unit)
    }

    override suspend fun stop(): Result<Unit> = runOnMain {
        val controller = playerController.controller()
        controller.stop()
        controller.clearMediaItems()
        Result.success(Unit)
    }

    override suspend fun setShuffleEnabled(enabled: Boolean): Result<Unit> = runOnMain {
        playerController.controller().setShuffleModeEnabled(enabled)
        Result.success(Unit)
    }

    override suspend fun setRepeatMode(mode: RepeatMode): Result<Unit> = runOnMain {
        playerController.controller().setRepeatMode(toMedia3RepeatMode(mode))
        Result.success(Unit)
    }

    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> = runOnMain {
        playerController.controller().setPlaybackSpeed(speed)
        Result.success(Unit)
    }

    override suspend fun setSleepTimer(minutes: Int?): Result<Unit> =
        Result.failure(UnsupportedOperationException("sleep timer lands in playback-05"))

    override suspend fun addToQueue(song: SongItem): Result<Unit> =
        Result.failure(UnsupportedOperationException("queue integration lands in playback-04"))

    override suspend fun removeFromQueue(songId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("queue integration lands in playback-04"))

    override suspend fun clearQueue(): Result<Unit> =
        Result.failure(UnsupportedOperationException("queue integration lands in playback-04"))

    /** Outer [ioDispatcher] hop gives callers non-blocking cancellation;
     * inner [Dispatchers.Main] hop satisfies Media3's main-thread rule.
     * Any throwable is converted to `Result.failure`. */
    private suspend fun runOnMain(block: suspend () -> Result<Unit>): Result<Unit> = try {
        withContext(ioDispatcher) {
            withContext(Dispatchers.Main) { block() }
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }

    // Mirror is the authoritative SongItem for whatever Media3 is playing.
    // playback-04 will index Map<MediaItem.mediaId, SongItem> here instead.
    private fun toSongItemOrNull(mediaItem: MediaItem?): SongItem? {
        if (mediaItem == null) return null
        return currentSongMirror.value
    }

    private fun toRepeatMode(repeatMode: Int): RepeatMode = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> RepeatMode.Off
        Player.REPEAT_MODE_ALL -> RepeatMode.All
        Player.REPEAT_MODE_ONE -> RepeatMode.One
        else -> RepeatMode.Off
    }

    private fun toMedia3RepeatMode(mode: RepeatMode): Int = when (mode) {
        RepeatMode.Off -> Player.REPEAT_MODE_OFF
        RepeatMode.All -> Player.REPEAT_MODE_ALL
        RepeatMode.One -> Player.REPEAT_MODE_ONE
    }

    // "mm:ss" formatter for elapsed/duration labels. Reserved for playback-05.
    private fun toProgressLabel(positionMs: Long, durationMs: Long): String {
        val safePosition = positionMs.coerceAtLeast(0L)
        val totalSeconds = safePosition / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
