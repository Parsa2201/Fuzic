package com.androidprj.fuzic.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.PlayerOverlay
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.PlayerRepository
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PlayerIntent {
    data class Play(val song: SongItem) : PlayerIntent
    data class PlayQueue(val songs: List<SongItem>, val startIndex: Int = 0) : PlayerIntent
    data object TogglePlayPause : PlayerIntent
    data object Previous : PlayerIntent
    data object Next : PlayerIntent
    data class Seek(val progress: Float) : PlayerIntent
    data object ToggleShuffle : PlayerIntent
    data object CycleRepeatMode : PlayerIntent
    data object ToggleLike : PlayerIntent
    data class ShowOverlay(val overlay: PlayerOverlay) : PlayerIntent
    data object DismissOverlay : PlayerIntent
    data class SleepTimerSelected(val minutes: Int?) : PlayerIntent
    data class PlaybackSpeedSelected(val speed: Float) : PlayerIntent
    data class QueueSongSelected(val song: SongItem) : PlayerIntent
    data class RemoveFromQueue(val song: SongItem) : PlayerIntent
    data object Stop : PlayerIntent
    data object ClearError : PlayerIntent
    data class Download(val song: SongItem) : PlayerIntent
    data class PlayById(val songId: String) : PlayerIntent
}

sealed interface PlayerUiEvent {
    data object NavigateToPremium : PlayerUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val interactionRepository: InteractionRepository,
    private val premiumRepository: PremiumRepository,
    private val downloadRepository: DownloadRepository,
    private val musicRepository: MusicRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _uiEvents = kotlinx.coroutines.channels.Channel<PlayerUiEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            var lastRepositoryError: String? = null
            playerRepository.playerState.collect { repositoryState ->
                val repoErrorChanged = lastRepositoryError != repositoryState.errorMessage
                lastRepositoryError = repositoryState.errorMessage
                _uiState.update { current ->
                    repositoryState.copy(
                        isLiked = if (repositoryState.currentSong?.id == current.currentSong?.id) {
                            current.isLiked
                        } else {
                            false
                        },
                        selectedOverlay = current.selectedOverlay,
                        errorMessage = if (repoErrorChanged) repositoryState.errorMessage else current.errorMessage,
                        visualizerAmplitudes = if (
                            repositoryState.isPlaying &&
                            repositoryState.currentSong?.id == current.currentSong?.id
                        ) {
                            current.visualizerAmplitudes
                        } else {
                            emptyList()
                        },
                        isPremiumUser = current.isPremiumUser,
                        actionErrorMessage = current.actionErrorMessage
                    )
                }
            }
        }
        viewModelScope.launch {
            playerRepository.playerState
                .map { it.currentSong?.id }
                .distinctUntilChanged()
                .collect { songId ->
                    val isLiked = songId?.let { id ->
                        withContext(ioDispatcher) { interactionRepository.isSongLiked(id).getOrDefault(false) }
                    } ?: false
                    _uiState.update { state ->
                        if (state.currentSong?.id == songId) state.copy(isLiked = isLiked) else state
                    }
                }
        }
        viewModelScope.launch {
            playerRepository.playerState
                .map { state -> state.isPlaying && state.currentSong != null }
                .distinctUntilChanged()
                .flatMapLatest { isActive ->
                    if (isActive) playerRepository.visualizerFrames else emptyFlow()
                }
                .collect { frame ->
                _uiState.update { state ->
                    state.copy(
                        visualizerAmplitudes = smoothAmplitudes(
                            previous = state.visualizerAmplitudes,
                            next = normalizeAmplitudes(frame.amplitudes),
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            premiumRepository.observePremiumStatus().collect { isPremium ->
                _uiState.update { it.copy(isPremiumUser = isPremium) }
            }
        }
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.Play -> runPlayerCommand { playerRepository.play(intent.song) }
            is PlayerIntent.PlayQueue -> runPlayerCommand { playerRepository.playQueue(intent.songs, intent.startIndex) }
            is PlayerIntent.PlayById -> playById(intent.songId)
            PlayerIntent.TogglePlayPause -> runPlayerCommand { playerRepository.togglePlayPause() }
            PlayerIntent.Previous -> runPlayerCommand { playerRepository.skipToPrevious() }
            PlayerIntent.Next -> runPlayerCommand { playerRepository.skipToNext() }
            is PlayerIntent.Seek -> runPlayerCommand { playerRepository.seekTo(intent.progress) }
            PlayerIntent.ToggleShuffle -> runPlayerCommand { playerRepository.setShuffleEnabled(!_uiState.value.isShuffleEnabled) }
            PlayerIntent.CycleRepeatMode -> runPlayerCommand { playerRepository.setRepeatMode(_uiState.value.repeatMode.next()) }
            PlayerIntent.ToggleLike -> toggleLike()
            is PlayerIntent.ShowOverlay -> _uiState.update { it.copy(selectedOverlay = intent.overlay) }
            PlayerIntent.DismissOverlay -> _uiState.update { it.copy(selectedOverlay = PlayerOverlay.None) }
            is PlayerIntent.SleepTimerSelected -> runPlayerCommand(PlayerOverlay.None) { playerRepository.setSleepTimer(intent.minutes) }
            is PlayerIntent.PlaybackSpeedSelected -> runPlayerCommand(PlayerOverlay.None) { playerRepository.setPlaybackSpeed(intent.speed) }
            is PlayerIntent.QueueSongSelected -> runPlayerCommand(PlayerOverlay.None) { playerRepository.play(intent.song) }
            is PlayerIntent.RemoveFromQueue -> runPlayerCommand { playerRepository.removeFromQueue(intent.song.id) }
            PlayerIntent.Stop -> runPlayerCommand { playerRepository.stop() }
            PlayerIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is PlayerIntent.Download -> download(intent.song)
        }
    }

    private fun playById(songId: String) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                musicRepository.getSongById(songId)
            }
            result.onSuccess { song ->
                if (song != null) {
                    runPlayerCommand { playerRepository.play(song) }
                } else {
                    _uiState.update { it.copy(errorMessage = stringProvider.get(R.string.player_error_title)) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: stringProvider.get(R.string.player_error_title)) }
            }
        }
    }

    private fun toggleLike() {
        val song = _uiState.value.currentSong ?: return
        val wasLiked = _uiState.value.isLiked
        _uiState.update { it.copy(isLiked = !wasLiked, errorMessage = null) }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                if (wasLiked) interactionRepository.unlikeSong(song.id) else interactionRepository.likeSong(song.id)
            }
            if (result.isFailure) _uiState.update { it.copy(isLiked = wasLiked, errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.player_error_title)) }
        }
    }

    private fun download(song: SongItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionErrorMessage = null) }
            val isPremium = premiumRepository.fetchPremiumStatus().getOrDefault(false)
            if (!isPremium) {
                _uiEvents.send(PlayerUiEvent.NavigateToPremium)
                return@launch
            }
            val request = DownloadRequest(
                song = song,
                audioUrl = song.audioUrl ?: ""
            )
            val result = withContext(ioDispatcher) {
                kotlin.runCatching { downloadRepository.enqueueDownload(request) }
            }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.player_error_title),
                    )
                }
            }
        }
    }

    private fun runPlayerCommand(
        overlayAfterSuccess: PlayerOverlay? = null,
        block: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { block() }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: stringProvider.get(R.string.player_error_title))
                }
            } else if (overlayAfterSuccess != null) {
                _uiState.update { it.copy(selectedOverlay = overlayAfterSuccess, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}

internal fun normalizeAmplitudes(
    amplitudes: List<Float>,
    barCount: Int = MAX_VISUALIZER_BARS,
): List<Float> {
    if (barCount <= 0) return emptyList()
    if (amplitudes.isEmpty()) return List(barCount) { 0f }

    return List(barCount) { index ->
        val sourceIndex = if (barCount == 1) 0 else {
            (index * (amplitudes.lastIndex.toFloat() / (barCount - 1))).toInt()
        }
        amplitudes[sourceIndex].coerceIn(0f, 1f)
    }
}

internal fun smoothAmplitudes(
    previous: List<Float>,
    next: List<Float>,
    smoothing: Float = VISUALIZER_SMOOTHING,
): List<Float> = next.mapIndexed { index, target ->
    val prior = previous.getOrNull(index) ?: target
    (prior + ((target - prior) * smoothing.coerceIn(0f, 1f))).coerceIn(0f, 1f)
}

private const val MAX_VISUALIZER_BARS = 32
private const val VISUALIZER_SMOOTHING = 0.35f

private fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.Off -> RepeatMode.All
    RepeatMode.All -> RepeatMode.One
    RepeatMode.One -> RepeatMode.Off
}
