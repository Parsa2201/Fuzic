package com.androidprj.fuzic.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidprj.fuzic.R
import com.androidprj.fuzic.di.IoDispatcher
import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.PlaylistCategory
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.PlaylistVisibility
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddToPlaylistUiState(
    val songId: String = "",
    val addedPlaylistIds: Set<String> = emptySet(),
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newPlaylistName: String = "",
    val newPlaylistCategory: PlaylistCategory = PlaylistCategory.Local,
    val newPlaylistVisibility: PlaylistVisibility = PlaylistVisibility.Private,
    val newPlaylistError: String? = null,
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
        val playlists = withContext(ioDispatcher) { playlistRepository.getUserPlaylists(userId) }
        val addedIds = withContext(ioDispatcher) { playlistRepository.getPlaylistIdsContainingSong(songId) }
        _uiState.value = playlists.fold(
            onSuccess = { items ->
                AddToPlaylistUiState(songId = songId, playlists = items, addedPlaylistIds = addedIds.getOrDefault(emptyList()).toSet(), isLoading = false)
            },
            onFailure = { error -> AddToPlaylistUiState(songId = songId, isLoading = false, errorMessage = error.message ?: stringProvider.get(R.string.add_to_playlist_error)) },
        )
    }

    fun toggleSongInPlaylist(playlist: PlaylistItem) = viewModelScope.launch {
        val songId = _uiState.value.songId
        if (songId.isBlank()) return@launch
        val wasAdded = playlist.id in _uiState.value.addedPlaylistIds
        _uiState.update { state -> state.copy(addedPlaylistIds = if (wasAdded) state.addedPlaylistIds - playlist.id else state.addedPlaylistIds + playlist.id) }
        val result = withContext(ioDispatcher) {
            if (wasAdded) playlistRepository.removeSongFromPlaylist(playlist.id, songId)
            else playlistRepository.addSongToPlaylist(playlist.id, songId)
        }
        if (result.isFailure) {
            _uiState.update { state -> state.copy(addedPlaylistIds = if (wasAdded) state.addedPlaylistIds + playlist.id else state.addedPlaylistIds - playlist.id) }
        }
    }

    fun showCreatePlaylist() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreatePlaylist() = _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "", newPlaylistError = null) }
    fun onNewPlaylistNameChange(name: String) = _uiState.update { it.copy(newPlaylistName = name, newPlaylistError = null) }
    fun onNewPlaylistCategoryChange(category: PlaylistCategory) = _uiState.update { it.copy(newPlaylistCategory = category) }
    fun onNewPlaylistVisibilityChange(visibility: PlaylistVisibility) = _uiState.update { it.copy(newPlaylistVisibility = visibility) }

    fun createPlaylist() = viewModelScope.launch {
        val state = _uiState.value
        if (state.newPlaylistName.isBlank()) return@launch
        withContext(ioDispatcher) {
            playlistRepository.createPlaylist(CreatePlaylistRequest(state.newPlaylistName, state.newPlaylistCategory, state.newPlaylistVisibility))
        }.fold(
            onSuccess = { created -> _uiState.update { it.copy(playlists = listOf(created) + it.playlists, showCreateDialog = false, newPlaylistName = "") } },
            onFailure = { error -> _uiState.update { it.copy(newPlaylistError = error.message) } },
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
    onNewPlaylistCategoryChange: (PlaylistCategory) -> Unit,
    onNewPlaylistVisibilityChange: (PlaylistVisibility) -> Unit,
    onCreatePlaylistSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = onHideCreatePlaylist,
            title = { Text(stringResource(R.string.playlists_create_title)) },
            text = { OutlinedTextField(value = newPlaylistName, onValueChange = onNewPlaylistNameChange, label = { Text(stringResource(R.string.playlists_name_label)) }, singleLine = true, isError = newPlaylistError != null, supportingText = { newPlaylistError?.let { Text(it) } }) },
            confirmButton = { Button(onClick = onCreatePlaylistSubmit) { Text(stringResource(R.string.action_create)) } },
            dismissButton = { TextButton(onClick = onHideCreatePlaylist) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.add_to_playlist_title), onBackClick = onBackClick, actions = {
            TextButton(onClick = onNewPlaylistClick) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.playlists_new_playlist)) }
        })
        when {
            errorMessage != null -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), errorMessage)
            !isLoading && playlists.isEmpty() -> ScreenMessage(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.add_to_playlist_title), stringResource(R.string.add_to_playlist_empty))
            else -> LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    val isAdded = playlist.id in addedPlaylistIds
                    ListItem(
                        headlineContent = { Text(playlist.title, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("${playlist.subtitle} • ${playlist.songCountLabel}", style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = { MusicArtwork(artworkUrl = playlist.artworkUrl, fallbackIcon = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = playlist.title, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) },
                        trailingContent = { IconButton(onClick = { onPlaylistClick(playlist) }) { Icon(if (isAdded) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline, contentDescription = null, tint = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } },
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylistClick(playlist) },
                    )
                }
            }
        }
    }
}
