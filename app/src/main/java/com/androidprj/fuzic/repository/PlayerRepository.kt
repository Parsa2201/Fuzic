package com.androidprj.fuzic.repository

import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val playerState: StateFlow<PlayerUiState>
    val visualizerFrames: Flow<AudioVisualizerFrame>
    suspend fun play(song: SongItem): Result<Unit>
    suspend fun playQueue(songs: List<SongItem>, startIndex: Int = 0): Result<Unit>
    suspend fun togglePlayPause(): Result<Unit>
    suspend fun seekTo(progress: Float): Result<Unit>
    suspend fun skipToPrevious(): Result<Unit>
    suspend fun skipToNext(): Result<Unit>
    suspend fun setShuffleEnabled(enabled: Boolean): Result<Unit>
    suspend fun setRepeatMode(mode: RepeatMode): Result<Unit>
    suspend fun setPlaybackSpeed(speed: Float): Result<Unit>
    suspend fun setSleepTimer(minutes: Int?): Result<Unit>
    suspend fun addToQueue(song: SongItem): Result<Unit>
    suspend fun removeFromQueue(songId: String): Result<Unit>
    suspend fun clearQueue(): Result<Unit>
    suspend fun stop(): Result<Unit>
}
