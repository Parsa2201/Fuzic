package com.androidprj.fuzic.ui.screens.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.model.ui.SongDetailsUiState
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.R
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.repository.MusicRepository
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.model.ui.DownloadRequest
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
    private val premiumRepository: PremiumRepository,
    private val downloadRepository: DownloadRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState: StateFlow<SongDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            premiumRepository.observePremiumStatus().collect { isPremium ->
                _uiState.value = _uiState.value.copy(isPremiumUser = isPremium)
            }
        }
    }

    fun load(songId: String) {
        viewModelScope.launch {
            _uiState.value = SongDetailsUiState(isLoading = true)
            val result = withContext(ioDispatcher) { musicRepository.getSongById(songId) }
            _uiState.value = result.fold(
                onSuccess = { song ->
                    val liked = withContext(ioDispatcher) { interactionRepository.isSongLiked(song.id) }
                    SongDetailsUiState(
                        song = song,
                        isLiked = liked.getOrDefault(false),
                        actionErrorMessage = liked.exceptionOrNull()?.let {
                            stringProvider.get(R.string.song_details_like_error)
                        },
                    )
                },
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
            _uiState.value = _uiState.value.copy(actionErrorMessage = null)
            val result = withContext(ioDispatcher) {
                if (wasLiked) interactionRepository.unlikeSong(song.id) else interactionRepository.likeSong(song.id)
            }
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLiked = !wasLiked, actionErrorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    actionErrorMessage = stringProvider.get(R.string.song_details_like_error),
                )
            }
        }
    }

    fun download() {
        val song = _uiState.value.song ?: return
        val url = song.audioUrl ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = stringProvider.get(R.string.song_details_error_title))
            return
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { downloadRepository.enqueueDownload(DownloadRequest(song, url)) }
            if (result.isFailure) _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
        }
    }
}
