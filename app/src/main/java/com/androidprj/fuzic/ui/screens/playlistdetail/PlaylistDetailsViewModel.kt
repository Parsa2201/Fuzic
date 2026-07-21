package com.androidprj.fuzic.ui.screens.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.PlaylistDetails
import com.androidprj.fuzic.model.ui.PlaylistDetailsUiState
import com.androidprj.fuzic.repository.PlayerRepository
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

sealed interface PlaylistDetailsIntent {
    data class Load(val playlistId: String) : PlaylistDetailsIntent
    data object Retry : PlaylistDetailsIntent
    data class PlayAll(val playlist: PlaylistDetails) : PlaylistDetailsIntent
    data object ClearError : PlaylistDetailsIntent
}

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val playlistDetailsRepository: PlaylistDetailsRepository,
    private val playerRepository: PlayerRepository,
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
            PlaylistDetailsIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    fun load(playlistId: String) {
        lastPlaylistId = playlistId
        viewModelScope.launch {
            _uiState.value = PlaylistDetailsUiState(isLoading = true)
            val result = withContext(ioDispatcher) { playlistDetailsRepository.getPlaylistDetails(playlistId) }
            _uiState.value = result.fold(
                onSuccess = { PlaylistDetailsUiState(playlist = it) },
                onFailure = {
                    PlaylistDetailsUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.playlist_details_error_title),
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
}
