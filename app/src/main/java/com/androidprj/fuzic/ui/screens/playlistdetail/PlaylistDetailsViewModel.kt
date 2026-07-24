package com.androidprj.fuzic.ui.screens.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.PlaylistDetails
import com.androidprj.fuzic.model.ui.PlaylistDetailsUiState
import com.androidprj.fuzic.model.ui.UpdatePlaylistRequest
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.PlayerRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.PlaylistDetailsRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.androidprj.fuzic.util.toUserFriendlyMessage

sealed interface PlaylistDetailsIntent {
    data class Load(val playlistId: String) : PlaylistDetailsIntent
    data object Retry : PlaylistDetailsIntent
    data class PlayAll(val playlist: PlaylistDetails) : PlaylistDetailsIntent
    data object ClearError : PlaylistDetailsIntent
    
    // CRUD Intents
    data object ShowEdit : PlaylistDetailsIntent
    data object DismissEdit : PlaylistDetailsIntent
    data class SaveEdit(val request: UpdatePlaylistRequest) : PlaylistDetailsIntent
    data object DeletePlaylist : PlaylistDetailsIntent
    data class RemoveSong(val songId: String) : PlaylistDetailsIntent
}

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistDetailsRepository: PlaylistDetailsRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerRepository: PlayerRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistDetailsUiState())
    val uiState: StateFlow<PlaylistDetailsUiState> = _uiState.asStateFlow()
    private var lastPlaylistId: String? = null

    fun onIntent(intent: PlaylistDetailsIntent) {
        when (intent) {
            is PlaylistDetailsIntent.Load -> load(intent.playlistId)
            PlaylistDetailsIntent.Retry -> lastPlaylistId?.let(::load)
            is PlaylistDetailsIntent.PlayAll -> playAll(intent.playlist)
            PlaylistDetailsIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null, actionErrorMessage = null)
            PlaylistDetailsIntent.ShowEdit -> _uiState.value = _uiState.value.copy(isEditing = true)
            PlaylistDetailsIntent.DismissEdit -> _uiState.value = _uiState.value.copy(isEditing = false)
            is PlaylistDetailsIntent.SaveEdit -> saveEdit(intent.request)
            PlaylistDetailsIntent.DeletePlaylist -> deletePlaylist()
            is PlaylistDetailsIntent.RemoveSong -> removeSong(intent.songId)
        }
    }

    fun load(playlistId: String) {
        lastPlaylistId = playlistId
        viewModelScope.launch {
            _uiState.value = PlaylistDetailsUiState(isLoading = true)
            val currentUserId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            val result = withContext(ioDispatcher) { playlistDetailsRepository.getPlaylistDetails(playlistId) }
            _uiState.value = result.fold(
                onSuccess = { 
                    PlaylistDetailsUiState(
                        playlist = it,
                        isOwner = it.ownerId == currentUserId
                    ) 
                },
                onFailure = {
                    PlaylistDetailsUiState(
                        errorMessage = it.toUserFriendlyMessage(stringProvider, R.string.playlist_details_error_title),
                    )
                },
            )
        }
    }

    private fun playAll(playlist: PlaylistDetails) {
        if (playlist.songs.isEmpty()) return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { playerRepository.playQueue(playlist.songs) }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                        ?: stringProvider.get(R.string.player_error_title),
                )
            }
        }
    }

    private fun saveEdit(request: UpdatePlaylistRequest) {
        val playlistId = lastPlaylistId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isEditing = false)
            val result = withContext(ioDispatcher) { playlistRepository.updatePlaylist(playlistId, request) }
            if (result.isSuccess) {
                load(playlistId) // reload to get new details
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to update playlist"
                )
            }
        }
    }

    private fun deletePlaylist() {
        val playlistId = lastPlaylistId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = withContext(ioDispatcher) { playlistRepository.deletePlaylist(playlistId) }
            if (result.isSuccess) {
                // To signal the UI that the playlist was deleted, we can set playlist to null
                _uiState.value = PlaylistDetailsUiState(playlist = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to delete playlist"
                )
            }
        }
    }

    private fun removeSong(songId: String) {
        val playlistId = lastPlaylistId ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
            if (result.isSuccess) {
                // Update local list
                val currentPlaylist = _uiState.value.playlist
                if (currentPlaylist != null) {
                    val newSongs = currentPlaylist.songs.filter { it.id != songId }
                    _uiState.value = _uiState.value.copy(
                        playlist = currentPlaylist.copy(songs = newSongs)
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Failed to remove song"
                )
            }
        }
    }
}
