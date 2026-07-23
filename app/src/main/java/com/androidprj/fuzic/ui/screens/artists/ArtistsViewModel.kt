package com.androidprj.fuzic.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.ArtistCollectionItem
import com.androidprj.fuzic.model.ui.ArtistCollectionUiState
import com.androidprj.fuzic.repository.ArtistRepository
import com.androidprj.fuzic.repository.FollowRepository
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

sealed interface ArtistsIntent {
    data object Retry : ArtistsIntent
    data object ClearError : ArtistsIntent
    data class ToggleFollow(val item: ArtistCollectionItem) : ArtistsIntent
}

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistCollectionUiState(isLoading = true))
    val uiState: StateFlow<ArtistCollectionUiState> = _uiState.asStateFlow()

    init {
        observeArtists()
    }

    fun onIntent(intent: ArtistsIntent) {
        when (intent) {
            ArtistsIntent.Retry -> observeArtists()
            ArtistsIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
            is ArtistsIntent.ToggleFollow -> toggleFollow(intent.item)
        }
    }

    internal fun setArtistsForTesting(artists: List<ArtistCollectionItem>) {
        _uiState.value = _uiState.value.copy(artists = artists, isLoading = false, errorMessage = null)
    }

    private fun observeArtists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                artistRepository.observeArtists().collect {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.toUserFriendlyMessage(stringProvider, R.string.artists_error_title),
                )
            }
        }
    }

    private fun toggleFollow(item: ArtistCollectionItem) {
        val nextFollowing = !item.isFollowing
        updateArtistFollowState(item.artist.id, nextFollowing)
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (item.isFollowing) {
                    followRepository.unfollowUser(item.artist.id)
                } else {
                    followRepository.followUser(item.artist.id)
                }
            }
            result.onFailure { throwable ->
                updateArtistFollowState(item.artist.id, item.isFollowing)
                _uiState.value = _uiState.value.copy(
                    errorMessage = throwable.toUserFriendlyMessage(stringProvider, R.string.artists_error_title),
                )
            }
        }
    }

    private fun updateArtistFollowState(artistId: String, isFollowing: Boolean) {
        _uiState.value = _uiState.value.copy(
            artists = _uiState.value.artists.map { item ->
                if (item.artist.id == artistId) {
                    item.copy(isFollowing = isFollowing)
                } else {
                    item
                }
            },
        )
    }
}
