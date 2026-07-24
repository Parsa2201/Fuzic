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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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

import kotlinx.coroutines.flow.update

data class AddToPlaylistUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isComplete: Boolean = false,
    val showCreateDialog: Boolean = false,
    val newPlaylistName: String = "",
    val newPlaylistError: String? = null
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

    fun showCreatePlaylist() { _uiState.update { it.copy(showCreateDialog = true) } }
    
    fun hideCreatePlaylist() { _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "", newPlaylistError = null) } }
    
    fun onNewPlaylistNameChange(name: String) { _uiState.update { it.copy(newPlaylistName = name, newPlaylistError = null) } }
    
    fun createPlaylist() = viewModelScope.launch {
        val name = _uiState.value.newPlaylistName
        if (name.isBlank()) return@launch
        val result = withContext(ioDispatcher) { 
            playlistRepository.createPlaylist(
                com.androidprj.fuzic.model.ui.CreatePlaylistRequest(
                    title = name, 
                    visibility = com.androidprj.fuzic.model.ui.PlaylistVisibility.Public
                )
            ) 
        }
        result.fold(
            onSuccess = { newPlaylist ->
                _uiState.update { 
                    it.copy(
                        playlists = listOf(newPlaylist) + it.playlists,
                        showCreateDialog = false,
                        newPlaylistName = ""
                    )
                }
            },
            onFailure = { err ->
                _uiState.update { it.copy(newPlaylistError = err.message) }
            }
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
            showCreateDialog = false,
            newPlaylistName = "",
            newPlaylistError = null,
            onBackClick = {},
            onPlaylistClick = {},
            onNewPlaylistClick = {},
            onHideCreatePlaylist = {},
            onNewPlaylistNameChange = {},
            onCreatePlaylistSubmit = {}
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
            showCreateDialog = false,
            newPlaylistName = "",
            newPlaylistError = null,
            onBackClick = {},
            onPlaylistClick = {},
            onNewPlaylistClick = {},
            onHideCreatePlaylist = {},
            onNewPlaylistNameChange = {},
            onCreatePlaylistSubmit = {}
        )
    }
}

@Composable
fun AddToPlaylistScreen(
    playlists: List<PlaylistItem>,
    isLoading: Boolean,
    errorMessage: String?,
    showCreateDialog: Boolean,
    newPlaylistName: String,
    newPlaylistError: String?,
    onBackClick: () -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onNewPlaylistClick: () -> Unit,
    onHideCreatePlaylist: () -> Unit,
    onNewPlaylistNameChange: (String) -> Unit,
    onCreatePlaylistSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onHideCreatePlaylist,
            title = { Text(stringResource(R.string.playlists_create_title)) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = onNewPlaylistNameChange,
                        label = { Text(stringResource(R.string.playlists_name_label)) },
                        singleLine = true,
                        isError = newPlaylistError != null,
                        supportingText = { if (newPlaylistError != null) Text(newPlaylistError) }
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = onCreatePlaylistSubmit) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onHideCreatePlaylist) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(
            title = stringResource(R.string.add_to_playlist_title), 
            onBackClick = onBackClick,
            actions = {
                androidx.compose.material3.TextButton(onClick = onNewPlaylistClick) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Add, 
                        contentDescription = null
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.playlists_new_playlist))
                }
            }
        )
        when {
            errorMessage != null -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), errorMessage)
            !isLoading && playlists.isEmpty() -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), stringResource(R.string.add_to_playlist_empty))
            else -> LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.title, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("${playlist.subtitle} • ${playlist.songCountLabel}", style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = {
                            com.androidprj.fuzic.ui.components.MusicArtwork(
                                artworkUrl = playlist.artworkUrl,
                                fallbackIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = playlist.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylistClick(playlist) },
                    )
                }
            }
        }
    }
}
