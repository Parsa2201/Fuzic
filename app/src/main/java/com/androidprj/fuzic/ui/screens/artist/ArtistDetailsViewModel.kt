package com.androidprj.fuzic.ui.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ArtistDetailsUiState
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.ArtistRepository
import com.androidprj.fuzic.repository.FollowRepository
import com.androidprj.fuzic.repository.PlayerRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ArtistDetailsIntent {
    data class Load(val artistId: String) : ArtistDetailsIntent
    data object Retry : ArtistDetailsIntent
    data object ToggleFollow : ArtistDetailsIntent
    data class PlaySong(val song: SongItem) : ArtistDetailsIntent
    data object ClearError : ArtistDetailsIntent
}

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val followRepository: FollowRepository,
    private val playerRepository: PlayerRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistDetailsUiState())
    val uiState: StateFlow<ArtistDetailsUiState> = _uiState.asStateFlow()
    private var lastArtistId: String? = null

    fun onIntent(intent: ArtistDetailsIntent) {
        when (intent) {
            is ArtistDetailsIntent.Load -> load(intent.artistId)
            ArtistDetailsIntent.Retry -> lastArtistId?.let(::load)
            ArtistDetailsIntent.ToggleFollow -> toggleFollow()
            is ArtistDetailsIntent.PlaySong -> playSong(intent.song)
            ArtistDetailsIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    fun load(artistId: String) {
        lastArtistId = artistId
        viewModelScope.launch {
            _uiState.value = ArtistDetailsUiState(isLoading = true)
            val result = withContext(ioDispatcher) { artistRepository.getArtistDetails(artistId) }
            _uiState.value = result.fold(
                onSuccess = {
                    ArtistDetailsUiState(
                        artist = it.artist,
                        popularSongs = it.popularSongs,
                    )
                },
                onFailure = {
                    ArtistDetailsUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.artist_error_title),
                    )
                },
            )
        }
    }

    private fun toggleFollow() {
        val artist: ArtistItem = _uiState.value.artist ?: return
        val wasFollowing = _uiState.value.isFollowing
        _uiState.value = _uiState.value.copy(isFollowing = !wasFollowing, errorMessage = null)
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (wasFollowing) followRepository.unfollowUser(artist.id) else followRepository.followUser(artist.id)
            }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isFollowing = wasFollowing,
                    errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.artist_error_title),
                )
            }
        }
    }

    private fun playSong(song: SongItem) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { playerRepository.play(song) }
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.player_error_title),
                )
            }
        }
    }
}
