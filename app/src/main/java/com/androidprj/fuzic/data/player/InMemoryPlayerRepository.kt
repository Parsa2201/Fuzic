package com.androidprj.fuzic.data.player

import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.PlayerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class InMemoryPlayerRepository @Inject constructor() : PlayerRepository {
    private val _playerState = MutableStateFlow(PlayerUiState())
    override val playerState: StateFlow<PlayerUiState> = _playerState
    override val visualizerFrames: Flow<AudioVisualizerFrame> = MutableSharedFlow()

    override suspend fun play(song: SongItem): Result<Unit> {
        _playerState.value = _playerState.value.copy(currentSong = song, isPlaying = true)
        return Result.success(Unit)
    }

    override suspend fun playQueue(songs: List<SongItem>, startIndex: Int): Result<Unit> {
        _playerState.value = _playerState.value.copy(currentSong = songs.getOrNull(startIndex), queue = songs, isPlaying = songs.isNotEmpty())
        return Result.success(Unit)
    }

    override suspend fun togglePlayPause(): Result<Unit> {
        _playerState.value = _playerState.value.copy(isPlaying = !_playerState.value.isPlaying)
        return Result.success(Unit)
    }

    override suspend fun seekTo(progress: Float): Result<Unit> {
        _playerState.value = _playerState.value.copy(progress = progress.coerceIn(0f, 1f))
        return Result.success(Unit)
    }

    override suspend fun skipToPrevious(): Result<Unit> = Result.success(Unit)
    override suspend fun skipToNext(): Result<Unit> = Result.success(Unit)
    override suspend fun setShuffleEnabled(enabled: Boolean): Result<Unit> {
        _playerState.value = _playerState.value.copy(isShuffleEnabled = enabled)
        return Result.success(Unit)
    }
    override suspend fun setRepeatMode(mode: RepeatMode): Result<Unit> {
        _playerState.value = _playerState.value.copy(repeatMode = mode)
        return Result.success(Unit)
    }
    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        return Result.success(Unit)
    }
    override suspend fun setSleepTimer(minutes: Int?): Result<Unit> {
        _playerState.value = _playerState.value.copy(sleepTimerMinutes = minutes)
        return Result.success(Unit)
    }
    override suspend fun addToQueue(song: SongItem): Result<Unit> {
        _playerState.value = _playerState.value.copy(queue = _playerState.value.queue + song)
        return Result.success(Unit)
    }
    override suspend fun removeFromQueue(songId: String): Result<Unit> {
        _playerState.value = _playerState.value.copy(queue = _playerState.value.queue.filterNot { it.id == songId })
        return Result.success(Unit)
    }
    override suspend fun clearQueue(): Result<Unit> {
        _playerState.value = _playerState.value.copy(queue = emptyList())
        return Result.success(Unit)
    }
    override suspend fun stop(): Result<Unit> {
        _playerState.value = PlayerUiState()
        return Result.success(Unit)
    }
}
