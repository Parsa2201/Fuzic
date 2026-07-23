package com.androidprj.fuzic.data.repository

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bridges Media3's [Player] callbacks to Fuzic's [PlayerUiState]. Kept in
 * its own file because the listener surface has grown large enough to
 * dominate the repository when inlined.
 *
 * The lookup `mediaItem.mediaId → SongItem` is delegated back to the
 * repository via [resolveSong] so the map's lifecycle stays close to the
 * transport commands that populate it.
 */
internal class Media3PlayerListener(
    private val playerState: MutableStateFlow<PlayerUiState>,
    private val currentSongMirror: MutableStateFlow<SongItem?>,
    private val resolveSong: (MediaItem?) -> SongItem?,
) : Player.Listener {

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playerState.update { it.copy(isPlaying = isPlaying, errorMessage = if (isPlaying) null else it.errorMessage) }
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
        // MediaItem is nullable on queue-end transitions; resolveSong
        // surfaces null in that case so the mini-player doesn't go stale.
        val resolvedSong = resolveSong(mediaItem)
        currentSongMirror.value = resolvedSong
        playerState.update { it.copy(currentSong = resolvedSong, errorMessage = null) }
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

    private fun toRepeatMode(repeatMode: Int): RepeatMode = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> RepeatMode.Off
        Player.REPEAT_MODE_ALL -> RepeatMode.All
        Player.REPEAT_MODE_ONE -> RepeatMode.One
        else -> RepeatMode.Off
    }
}
