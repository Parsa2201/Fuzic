package com.androidprj.fuzic.data.repository

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.player.PlayerController
import com.androidprj.fuzic.player.queue.PlaybackQueue
import com.androidprj.fuzic.player.timer.SleepTimer
import com.androidprj.fuzic.repository.PlayerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
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
 * controller API is main-thread only. Visualizer frames (playback-06) still
 * intentionally return an empty flow; the sleep timer (playback-05) and
 * 250 ms progress polling ticker are wired here.
 */
@Singleton
class Media3PlayerRepository @Inject constructor(
    private val playerController: PlayerController,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    initialPlaybackQueue: PlaybackQueue = PlaybackQueue(),
) : PlayerRepository {

    // Exposed as MutableStateFlow so the listener can update the snapshot
    // directly; external callers see only StateFlow via the interface.
    override val playerState: MutableStateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState())

    override val visualizerFrames: Flow<AudioVisualizerFrame> = emptyFlow()

    // Authoritative SongItem mirror for whatever Media3 is playing.
    private val currentSongMirror: MutableStateFlow<SongItem?> = MutableStateFlow(null)

    // Logical queue state. `initialPlaybackQueue` is a constructor arg
    // without the `val` keyword because we hold a mutable reference
    // internally — Hilt still satisfies the binding by constructing the
    // default `PlaybackQueue()`.
    private var playbackQueue: PlaybackQueue = initialPlaybackQueue

    // Mirror of the logical queue contents for tests / future observers.
    private val queueMirror: MutableStateFlow<List<SongItem>> =
        MutableStateFlow(playbackQueue.songs)

    // Reverse lookup: MediaItem.mediaId → SongItem. Populated by every
    // transport command that adds a song so onMediaItemTransition can
    // recover the full SongItem without stashing separate mirrors.
    private val songByMediaId: MutableMap<String, SongItem> = mutableMapOf()

    // Process-scoped listener scope — survives every ViewModel and screen
    // rotation so playback callbacks keep updating state during navigation.
    @OptIn(DelicateCoroutinesApi::class)
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val listener: Media3PlayerListener = Media3PlayerListener(
        playerState = playerState,
        currentSongMirror = currentSongMirror,
        resolveSong = ::toSongItemOrNull,
    )

    // Sleep-timer mirror. Held as a flow so future analytics/state-sync
    // observers don't need to scrape PlayerUiState. Updated on every
    // setSleepTimer(...) call (immediately after the expiry callback or
    // cancel command) so the UI's "stop in N minutes" chip stays in sync.
    private val sleepTimerMinutesMirror: MutableStateFlow<Int?> = MutableStateFlow(null)

    // Sleep timer helper. Owns a single replaceable Job in the repository
    // scope; expiry pauses playback via the PlayerController. Not a Hilt
    // binding because it needs both the long-lived scope above and a
    // callback that hops to Main before calling Media3's controller.
    private val sleepTimer: SleepTimer = SleepTimer(
        scope = scope,
        onExpire = ::pausePlaybackForSleepTimer,
    )

    // 250 ms progress-polling ticker. Lives in its own file so the
    // repository stays under the 350-line soft cap; restartable via
    // Media3ProgressPoller#start if external code ever needs to reset
    // the loop.
    private val progressPoller = Media3ProgressPoller(
        playerController = playerController,
        playerState = playerState,
    )

    init {
        scope.launch { playerController.addListener(listener) }
        progressPoller.start(scope)
    }

    override suspend fun play(song: SongItem): Result<Unit> = runOnMain {
        val url = song.audioUrl
        if (url.isNullOrEmpty()) {
            return@runOnMain Result.failure(IllegalArgumentException("song has no audioUrl"))
        }
        val controller = playerController.controller()
        // Stash the SongItem before issuing transport commands so the
        // subsequent onMediaItemTransition callback can recover it.
        songByMediaId[song.id] = song
        currentSongMirror.value = song
        controller.setMediaItem(buildMediaItem(song), 0L)
        controller.prepare()
        controller.play()
        playbackQueue = playbackQueue.withSongs(listOf(song), 0)
        queueMirror.value = playbackQueue.songs
        playerState.update {
            it.copy(currentSong = song, queue = playbackQueue.songs)
        }
        Result.success(Unit)
    }

    override suspend fun playQueue(
        songs: List<SongItem>,
        startIndex: Int,
    ): Result<Unit> = runOnMain {
        if (songs.isEmpty()) {
            return@runOnMain Result.failure(
                IllegalArgumentException("playQueue called with empty list"),
            )
        }
        val playable = songs.filter { !it.audioUrl.isNullOrEmpty() }
        if (playable.isEmpty()) {
            return@runOnMain Result.failure(
                IllegalArgumentException("playQueue has no songs with audioUrl"),
            )
        }
        val safeIndex = startIndex.coerceIn(0, playable.lastIndex)
        val mediaItems = playable.map(::buildMediaItem)
        val controller = playerController.controller()
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
        playable.forEach { song -> songByMediaId[song.id] = song }
        val anchorSong = playable[safeIndex]
        playbackQueue = playbackQueue.withSongs(playable, safeIndex)
        queueMirror.value = playbackQueue.songs
        currentSongMirror.value = anchorSong
        playerState.update {
            it.copy(currentSong = anchorSong, queue = playbackQueue.songs)
        }
        Result.success(Unit)
    }

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
        // Per spec: update Media3 first, then the logical queue. Media3's
        // shuffle mode affects what its listener / lock screen surfaces;
        // our PlaybackQueue tracks the same toggle for UI display.
        playerController.controller().setShuffleModeEnabled(enabled)
        playbackQueue = playbackQueue.withShuffleEnabled(enabled)
        queueMirror.value = playbackQueue.songs
        playerState.update {
            it.copy(isShuffleEnabled = enabled, queue = playbackQueue.songs)
        }
        Result.success(Unit)
    }

    override suspend fun setRepeatMode(mode: RepeatMode): Result<Unit> = runOnMain {
        playerController.controller().setRepeatMode(toMedia3RepeatMode(mode))
        playbackQueue = playbackQueue.withRepeatMode(mode)
        playerState.update { it.copy(repeatMode = mode) }
        Result.success(Unit)
    }

    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> = runOnMain {
        playerController.controller().setPlaybackSpeed(speed)
        Result.success(Unit)
    }

    override suspend fun setSleepTimer(minutes: Int?): Result<Unit> = runOnMain {
        if (minutes == null) {
            // The user explicitly turned the timer off — cancel any armed
            // job, then drop both mirrors back to null.
            sleepTimer.cancel()
            sleepTimerMinutesMirror.value = null
            playerState.update { it.copy(sleepTimerMinutes = null) }
            return@runOnMain Result.success(Unit)
        }
        // Update the UI *before* arming the timer so the chip renders
        // immediately even when the user picks a long duration. The
        // expiry callback below resets both mirrors atomically.
        sleepTimerMinutesMirror.value = minutes
        playerState.update { it.copy(sleepTimerMinutes = minutes) }
        sleepTimer.start(
            minutes = minutes,
            onExpireResult = { result ->
                // Clear the mirrors regardless of success/failure of the
                // expiry callback itself — the user expects the chip to
                // disappear once the timer has fired.
                sleepTimerMinutesMirror.value = null
                playerState.update { it.copy(sleepTimerMinutes = null) }
            },
        )
        Result.success(Unit)
    }

    override suspend fun addToQueue(song: SongItem): Result<Unit> = runOnMain {
        val url = song.audioUrl
        if (url.isNullOrEmpty()) {
            return@runOnMain Result.failure(
                IllegalArgumentException("song has no audioUrl"),
            )
        }
        val controller = playerController.controller()
        controller.addMediaItem(buildMediaItem(song))
        songByMediaId[song.id] = song
        playbackQueue = playbackQueue.withAddedSong(song)
        queueMirror.value = playbackQueue.songs
        playerState.update { it.copy(queue = playbackQueue.songs) }
        Result.success(Unit)
    }

    override suspend fun removeFromQueue(songId: String): Result<Unit> = runOnMain {
        val controller = playerController.controller()
        val mediaIndex = findMediaItemIndex(controller, songId)
        if (mediaIndex < 0) {
            return@runOnMain Result.failure(
                NoSuchElementException("song $songId not in queue"),
            )
        }
        controller.removeMediaItem(mediaIndex)
        songByMediaId.remove(songId)
        playbackQueue = playbackQueue.withRemovedSong(songId)
        queueMirror.value = playbackQueue.songs
        playerState.update { it.copy(queue = playbackQueue.songs) }
        Result.success(Unit)
    }

    override suspend fun clearQueue(): Result<Unit> = runOnMain {
        val controller = playerController.controller()
        controller.clearMediaItems()
        songByMediaId.clear()
        playbackQueue = playbackQueue.withCleared()
        queueMirror.value = emptyList()
        playerState.update { it.copy(queue = emptyList()) }
        Result.success(Unit)
    }

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

    /** Expire callback for the [SleepTimer]. Hops to Main because
     * MediaController#pause is main-thread only. Failure here is allowed
     * to bubble — SleepTimer reports it via its onExpireResult callback,
     * which the repository uses to clear its mirrors. */
    private suspend fun pausePlaybackForSleepTimer() {
        withContext(Dispatchers.Main) {
            playerController.controller().pause()
        }
    }

    // Recover the original SongItem from the MediaItem Media3 just handed us.
    // PlaybackQueue is the source of truth for *which* songs exist; the map
    // provides the lookup key (MediaItem.mediaId is the song id).
    private fun toSongItemOrNull(mediaItem: MediaItem?): SongItem? {
        val id = mediaItem?.mediaId ?: return null
        return songByMediaId[id]
    }

    // Centralises MediaItem construction so every entry point populates the
    // songByMediaId map with the same mediaId convention (== SongItem.id).
    private fun buildMediaItem(song: SongItem): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.audioUrl)
            .build()

    // Linear scan over Media3's media item list — queues are small enough
    // (hundreds at most) that this is cheaper than maintaining a parallel
    // structure.
    private fun findMediaItemIndex(controller: Player, songId: String): Int {
        for (i in 0 until controller.mediaItemCount) {
            if (controller.getMediaItemAt(i).mediaId == songId) {
                return i
            }
        }
        return -1
    }

    private fun toMedia3RepeatMode(mode: RepeatMode): Int = when (mode) {
        RepeatMode.Off -> Player.REPEAT_MODE_OFF
        RepeatMode.All -> Player.REPEAT_MODE_ALL
        RepeatMode.One -> Player.REPEAT_MODE_ONE
    }
}
