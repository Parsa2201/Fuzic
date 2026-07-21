package com.androidprj.fuzic.ui.screens.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.model.ui.SongDetailsUiState
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.R
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SongDetailsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val interactionRepository: InteractionRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState: StateFlow<SongDetailsUiState> = _uiState.asStateFlow()

    fun load(songId: String) {
        viewModelScope.launch {
            _uiState.value = SongDetailsUiState(isLoading = true)
            val result = withContext(ioDispatcher) { musicRepository.getSongById(songId) }
            _uiState.value = result.fold(
                onSuccess = { SongDetailsUiState(song = it) },
                onFailure = {
                    SongDetailsUiState(
                        errorMessage = it.message ?: stringProvider.get(R.string.song_details_error_title),
                    )
                },
            )
        }
    }

    fun toggleLike() {
        val song = _uiState.value.song ?: return
        val wasLiked = _uiState.value.isLiked
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            val result = withContext(ioDispatcher) {
                if (wasLiked) interactionRepository.unlikeSong(song.id) else interactionRepository.likeSong(song.id)
            }
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLiked = !wasLiked)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                        ?: stringProvider.get(R.string.song_details_error_title),
                )
            }
        }
    }
}
