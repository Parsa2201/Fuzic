package com.androidprj.fuzic.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.util.StringProvider
import com.androidprj.fuzic.ui.theme.FuzicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddToPlaylistUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isComplete: Boolean = false,
)

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val playlistRepository: PlaylistRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddToPlaylistUiState())
    val uiState: StateFlow<AddToPlaylistUiState> = _uiState.asStateFlow()

    fun load() = viewModelScope.launch {
        _uiState.value = AddToPlaylistUiState()
        val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
        if (userId == null) {
            _uiState.value = AddToPlaylistUiState(isLoading = false, errorMessage = stringProvider.get(R.string.auth_error_message))
            return@launch
        }
        val result = withContext(ioDispatcher) { playlistRepository.getUserPlaylists(userId) }
        _uiState.value = result.fold(
            onSuccess = { AddToPlaylistUiState(playlists = it, isLoading = false) },
            onFailure = { AddToPlaylistUiState(isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.add_to_playlist_error)) },
        )
    }

    fun addSong(playlist: PlaylistItem, songId: String) = viewModelScope.launch {
        val result = withContext(ioDispatcher) { playlistRepository.addSongToPlaylist(playlist.id, songId) }
        _uiState.value = result.fold(
            onSuccess = { _uiState.value.copy(isComplete = true, errorMessage = null) },
            onFailure = { _uiState.value.copy(errorMessage = it.message ?: stringProvider.get(R.string.add_to_playlist_error)) },
        )
    }
}

@Preview(name = "Add to playlist - English", showBackground = true)
@Composable
private fun AddToPlaylistEnglishPreview() {
    FuzicTheme {
        AddToPlaylistScreen(
            playlists = listOf(
                PlaylistItem("night-drive", "Night Drive", "Parsa", "18 songs"),
                PlaylistItem("focus", "Focus", "Parsa", "24 songs"),
            ),
            isLoading = false,
            errorMessage = null,
            onBackClick = {},
            onPlaylistClick = {},
        )
    }
}

@Preview(name = "Add to playlist empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun AddToPlaylistEmptyPersianPreview() {
    FuzicTheme {
        AddToPlaylistScreen(
            playlists = emptyList(),
            isLoading = false,
            errorMessage = null,
            onBackClick = {},
            onPlaylistClick = {},
        )
    }
}

@Composable
fun AddToPlaylistScreen(
    playlists: List<PlaylistItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(stringResource(R.string.add_to_playlist_title), onBackClick)
        when {
            errorMessage != null -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), errorMessage)
            !isLoading && playlists.isEmpty() -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), stringResource(R.string.add_to_playlist_empty))
            else -> LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.title) },
                        supportingContent = { Text(playlist.subtitle) },
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylistClick(playlist) },
                    )
                }
            }
        }
    }
}
