package com.androidprj.fuzic.ui.screens.songcollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.SongCollectionUiState
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SongCollectionIntent {
    data object Retry : SongCollectionIntent
    data object ClearError : SongCollectionIntent
    data class Remove(val songId: String) : SongCollectionIntent
}

@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val interactionRepository: InteractionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCollectionUiState(isLoading = true))
    val uiState: StateFlow<SongCollectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: SongCollectionIntent) {
        when (intent) {
            SongCollectionIntent.Retry -> load()
            SongCollectionIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
            is SongCollectionIntent.Remove -> viewModelScope.launch { interactionRepository.unlikeSong(intent.songId).onSuccess { _uiState.value = _uiState.value.copy(songs = _uiState.value.songs.filterNot { it.id == intent.songId }) } }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = SongCollectionUiState(isLoading = true)
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            if (userId == null) {
                _uiState.value = SongCollectionUiState(
                    errorMessage = stringProvider.get(R.string.auth_error_message),
                )
                return@launch
            }
            val result = withContext(ioDispatcher) { interactionRepository.getLikedSongs(userId) }
            _uiState.value = result.fold(
                onSuccess = { SongCollectionUiState(songs = it) },
                onFailure = {
                    SongCollectionUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.liked_songs_error_title),
                    )
                },
            )
        }
    }
}

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val interactionRepository: InteractionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCollectionUiState(isLoading = true))
    val uiState: StateFlow<SongCollectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: SongCollectionIntent) {
        when (intent) {
            SongCollectionIntent.Retry -> load()
            SongCollectionIntent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
            is SongCollectionIntent.Remove -> viewModelScope.launch { interactionRepository.removeRecentlyPlayed(intent.songId).onSuccess { _uiState.value = _uiState.value.copy(songs = _uiState.value.songs.filterNot { it.id == intent.songId }) } }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = SongCollectionUiState(isLoading = true)
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            if (userId == null) {
                _uiState.value = SongCollectionUiState(
                    errorMessage = stringProvider.get(R.string.auth_error_message),
                )
                return@launch
            }
            val result = withContext(ioDispatcher) { interactionRepository.getRecentlyPlayed(userId) }
            _uiState.value = result.fold(
                onSuccess = { SongCollectionUiState(songs = it) },
                onFailure = {
                    SongCollectionUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.recently_played_error_title),
                    )
                },
            )
        }
    }
}
