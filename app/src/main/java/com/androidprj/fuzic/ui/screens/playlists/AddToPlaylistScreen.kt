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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
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
    val songId: String = "",
    val addedPlaylistIds: Set<String> = emptySet(),
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newPlaylistName: String = "",
    val newPlaylistCategory: com.androidprj.fuzic.model.ui.PlaylistCategory = com.androidprj.fuzic.model.ui.PlaylistCategory.Local,
    val newPlaylistVisibility: com.androidprj.fuzic.model.ui.PlaylistVisibility = com.androidprj.fuzic.model.ui.PlaylistVisibility.Private,
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

    fun load(songId: String) = viewModelScope.launch {
        _uiState.value = AddToPlaylistUiState(songId = songId)
        val userId = withContext(ioDispatcher) { authRepository.getCurrentUserId() }
        if (userId == null) {
            _uiState.value = AddToPlaylistUiState(isLoading = false, errorMessage = stringProvider.get(R.string.auth_error_message))
            return@launch
        }
        val playlistsResult = withContext(ioDispatcher) { playlistRepository.getUserPlaylists(userId) }
        val addedResult = withContext(ioDispatcher) { playlistRepository.getPlaylistIdsContainingSong(songId) }
        
        _uiState.value = playlistsResult.fold(
            onSuccess = { playlists -> 
                val addedIds = addedResult.getOrNull()?.toSet() ?: emptySet()
                AddToPlaylistUiState(
                    songId = songId,
                    playlists = playlists, 
                    addedPlaylistIds = addedIds,
                    isLoading = false
                ) 
            },
            onFailure = { AddToPlaylistUiState(songId = songId, isLoading = false, errorMessage = it.message ?: stringProvider.get(R.string.add_to_playlist_error)) },
        )
    }

    fun toggleSongInPlaylist(playlist: PlaylistItem) = viewModelScope.launch {
        val songId = _uiState.value.songId
        if (songId.isEmpty()) return@launch
        
        val isAdded = _uiState.value.addedPlaylistIds.contains(playlist.id)
        if (isAdded) {
            // Optimistic update
            _uiState.update { state -> 
                state.copy(
                    addedPlaylistIds = state.addedPlaylistIds - playlist.id,
                    playlists = state.playlists.map { 
                        if (it.id == playlist.id) it.copy(songCountLabel = decrementSongCount(it.songCountLabel)) else it
                    }
                )
            }
            val result = withContext(ioDispatcher) { playlistRepository.removeSongFromPlaylist(playlist.id, songId) }
            result.onFailure {
                // Revert update on failure
                _uiState.update { state -> 
                    state.copy(
                        addedPlaylistIds = state.addedPlaylistIds + playlist.id,
                        playlists = state.playlists.map { 
                            if (it.id == playlist.id) it.copy(songCountLabel = incrementSongCount(it.songCountLabel)) else it
                        }
                    )
                }
            }
        } else {
            // Optimistic update
            _uiState.update { state -> 
                state.copy(
                    addedPlaylistIds = state.addedPlaylistIds + playlist.id,
                    playlists = state.playlists.map { 
                        if (it.id == playlist.id) it.copy(songCountLabel = incrementSongCount(it.songCountLabel)) else it
                    }
                )
            }
            val result = withContext(ioDispatcher) { playlistRepository.addSongToPlaylist(playlist.id, songId) }
            result.onFailure {
                // Revert update on failure
                _uiState.update { state -> 
                    state.copy(
                        addedPlaylistIds = state.addedPlaylistIds - playlist.id,
                        playlists = state.playlists.map { 
                            if (it.id == playlist.id) it.copy(songCountLabel = decrementSongCount(it.songCountLabel)) else it
                        }
                    )
                }
            }
        }
    }
    
    private fun incrementSongCount(label: String): String {
        val count = label.substringBefore(" ").toIntOrNull() ?: 0
        return "${count + 1} songs"
    }

    private fun decrementSongCount(label: String): String {
        val count = label.substringBefore(" ").toIntOrNull() ?: 0
        return "${maxOf(0, count - 1)} songs"
    }

    fun showCreatePlaylist() { _uiState.update { it.copy(showCreateDialog = true) } }
    
    fun hideCreatePlaylist() { _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "", newPlaylistError = null) } }
    
    fun onNewPlaylistNameChange(name: String) { _uiState.update { it.copy(newPlaylistName = name, newPlaylistError = null) } }
    fun onNewPlaylistCategoryChange(category: com.androidprj.fuzic.model.ui.PlaylistCategory) { _uiState.update { it.copy(newPlaylistCategory = category) } }
    fun onNewPlaylistVisibilityChange(visibility: com.androidprj.fuzic.model.ui.PlaylistVisibility) { _uiState.update { it.copy(newPlaylistVisibility = visibility) } }
    
    fun createPlaylist() = viewModelScope.launch {
        val name = _uiState.value.newPlaylistName
        if (name.isBlank()) return@launch
        val result = withContext(ioDispatcher) { 
            playlistRepository.createPlaylist(
                com.androidprj.fuzic.model.ui.CreatePlaylistRequest(
                    title = name, 
                    category = _uiState.value.newPlaylistCategory,
                    visibility = _uiState.value.newPlaylistVisibility
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
            songId = "song1",
            addedPlaylistIds = setOf("night-drive"),
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
            onNewPlaylistCategoryChange = {},
            onNewPlaylistVisibilityChange = {},
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
            songId = "",
            addedPlaylistIds = emptySet(),
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
            onNewPlaylistCategoryChange = {},
            onNewPlaylistVisibilityChange = {},
            onNewPlaylistNameChange = {},
            onCreatePlaylistSubmit = {}
        )
    }
}

@Composable
fun AddToPlaylistScreen(
    songId: String,
    addedPlaylistIds: Set<String>,
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
    onNewPlaylistCategoryChange: (com.androidprj.fuzic.model.ui.PlaylistCategory) -> Unit,
    onNewPlaylistVisibilityChange: (com.androidprj.fuzic.model.ui.PlaylistVisibility) -> Unit,
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
                    val isAdded = addedPlaylistIds.contains(playlist.id)
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
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = { onPlaylistClick(playlist) }) {
                                if (isAdded) {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                        contentDescription = "Added",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.AddCircleOutline,
                                        contentDescription = "Add"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylistClick(playlist) },
                    )
                }
            }
        }
    }
}
