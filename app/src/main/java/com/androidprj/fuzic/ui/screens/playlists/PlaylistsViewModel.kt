package com.androidprj.fuzic.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.model.ui.CreatePlaylistUiState
import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistSection
import com.androidprj.fuzic.model.ui.PlaylistSectionType
import com.androidprj.fuzic.model.ui.PlaylistsUiState
import com.androidprj.fuzic.model.ui.PlaylistCategory
import com.androidprj.fuzic.model.ui.PlaylistVisibility
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PlaylistsIntent {
    data object Retry : PlaylistsIntent
    data object ShowCreate : PlaylistsIntent
    data object DismissCreate : PlaylistsIntent
    data class NameChanged(val value: String) : PlaylistsIntent
    data class CoverChanged(val value: String?) : PlaylistsIntent
    data class CategoryChanged(val value: PlaylistCategory) : PlaylistsIntent
    data class VisibilityChanged(val value: PlaylistVisibility) : PlaylistsIntent
    data object Create : PlaylistsIntent
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: PlaylistsIntent) {
        when (intent) {
            PlaylistsIntent.Retry -> load()
            PlaylistsIntent.ShowCreate -> _uiState.update { it.copy(createPlaylistState = it.createPlaylistState.copy(isVisible = true)) }
            PlaylistsIntent.DismissCreate -> _uiState.update { it.copy(createPlaylistState = CreatePlaylistUiState()) }
            is PlaylistsIntent.NameChanged -> _uiState.update {
                it.copy(createPlaylistState = it.createPlaylistState.copy(name = intent.value, hasNameConflict = false))
            }
            is PlaylistsIntent.CoverChanged -> _uiState.update {
                it.copy(createPlaylistState = it.createPlaylistState.copy(selectedCoverUri = intent.value))
            }
            PlaylistsIntent.Create -> createPlaylist()
            is PlaylistsIntent.CategoryChanged -> _uiState.update { it.copy(createPlaylistState = it.createPlaylistState.copy(category = intent.value)) }
            is PlaylistsIntent.VisibilityChanged -> _uiState.update { it.copy(createPlaylistState = it.createPlaylistState.copy(visibility = intent.value)) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
            if (userId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = stringProvider.get(R.string.auth_error_message),
                    )
                }
                return@launch
            }
            val result = withContext(ioDispatcher) {
                coroutineScope {
                    val global = async { playlistRepository.getGlobalPlaylists() }
                    val local = async { playlistRepository.getLocalPlaylists() }
                    val mine = async { playlistRepository.getUserPlaylists(userId) }
                    PlaylistLoadResult(
                        global = global.await(),
                        local = local.await(),
                        mine = mine.await(),
                    )
                }
            }
            val failure = listOf(result.global, result.local, result.mine).firstOrNull { it.isFailure }
            if (failure != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = failure.exceptionOrNull()?.message
                            ?: stringProvider.get(R.string.playlists_error_message),
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sections = listOf(
                        PlaylistSection(R.string.playlists_section_global, PlaylistSectionType.Global, result.global.getOrThrow()),
                        PlaylistSection(R.string.playlists_section_local, PlaylistSectionType.Local, result.local.getOrThrow()),
                        PlaylistSection(R.string.playlists_section_mine, PlaylistSectionType.Mine, result.mine.getOrThrow()),
                    ),
                )
            }
        }
    }

    private fun createPlaylist() {
        val createState = _uiState.value.createPlaylistState
        val title = createState.name.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() } ?: return@launch
            val existingNames = _uiState.value.sections
                .firstOrNull { it.type == PlaylistSectionType.Mine }
                ?.playlists
                ?.map { it.title.lowercase() }
                .orEmpty()
            if (title.lowercase() in existingNames) {
                _uiState.update { it.copy(createPlaylistState = it.createPlaylistState.copy(hasNameConflict = true)) }
                return@launch
            }
            val result = withContext(ioDispatcher) {
                playlistRepository.createPlaylist(
                    CreatePlaylistRequest(
                        title = title,
                        category = createState.category,
                        visibility = createState.visibility,
                        coverImageUrl = createState.selectedCoverUri,
                    )
                )
            }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message
                            ?: stringProvider.get(R.string.playlists_error_message),
                    )
                }
            } else {
                load()
            }
        }
    }
}

private data class PlaylistLoadResult(
    val global: Result<List<com.androidprj.fuzic.model.ui.PlaylistItem>>,
    val local: Result<List<com.androidprj.fuzic.model.ui.PlaylistItem>>,
    val mine: Result<List<com.androidprj.fuzic.model.ui.PlaylistItem>>,
)
