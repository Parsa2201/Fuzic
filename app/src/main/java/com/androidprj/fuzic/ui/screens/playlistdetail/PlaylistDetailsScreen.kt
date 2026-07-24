package com.androidprj.fuzic.ui.screens.playlistdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.PlaylistDetails
import com.androidprj.fuzic.model.ui.PlaylistDetailsUiState
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailArtwork
import com.androidprj.fuzic.ui.components.DetailLoadingContent
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SongListItem
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsRoute(
    uiState: PlaylistDetailsUiState,
    onBackClick: () -> Unit,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRemoveSongClick: (SongItem) -> Unit,
    onSaveEdit: (String, String?, com.androidprj.fuzic.model.ui.PlaylistCategory, com.androidprj.fuzic.model.ui.PlaylistVisibility) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaylistDetailsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPlayAllClick = onPlayAllClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRemoveSongClick = onRemoveSongClick,
        onSaveEdit = onSaveEdit,
        onDeletePlaylist = onDeletePlaylist,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    uiState: PlaylistDetailsUiState,
    onBackClick: () -> Unit,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRemoveSongClick: (SongItem) -> Unit,
    onSaveEdit: (String, String?, com.androidprj.fuzic.model.ui.PlaylistCategory, com.androidprj.fuzic.model.ui.PlaylistVisibility) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    var editName by androidx.compose.runtime.remember(uiState.playlist?.title) { 
        androidx.compose.runtime.mutableStateOf(uiState.playlist?.title ?: "") 
    }
    var editCover by androidx.compose.runtime.remember(uiState.playlist?.artworkUrl) { 
        androidx.compose.runtime.mutableStateOf(uiState.playlist?.artworkUrl) 
    }
    var editCategory by androidx.compose.runtime.remember(uiState.playlist?.category) {
        androidx.compose.runtime.mutableStateOf(uiState.playlist?.category ?: com.androidprj.fuzic.model.ui.PlaylistCategory.Local)
    }
    var editVisibility by androidx.compose.runtime.remember(uiState.playlist?.visibility) {
        androidx.compose.runtime.mutableStateOf(uiState.playlist?.visibility ?: com.androidprj.fuzic.model.ui.PlaylistVisibility.Private)
    }
    
    val coverPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) editCover = uri.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(
            title = stringResource(R.string.playlist_details_title),
            onBackClick = onBackClick,
            actions = {
                if (uiState.isOwner) {
                    androidx.compose.material3.IconButton(onClick = { showEditDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Playlist")
                    }
                    androidx.compose.material3.IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete Playlist")
                    }
                }
            }
        )
        when {
            uiState.isLoading -> DetailLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.playlist_details_error_title),
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
            uiState.playlist == null -> ScreenMessage(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = stringResource(R.string.playlist_details_error_title),
                message = stringResource(R.string.playlist_details_empty_message),
            )
            else -> PlaylistDetailsContent(
                playlist = uiState.playlist,
                onPlayAllClick = onPlayAllClick,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                onRemoveSongClick = onRemoveSongClick,
            )
        }
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete this playlist? This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePlaylist()
                        onBackClick() // navigate back immediately
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showEditDialog && uiState.playlist != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    var categoryExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = editCategory.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            com.androidprj.fuzic.model.ui.PlaylistCategory.values().forEach { category ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        editCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (editCategory != com.androidprj.fuzic.model.ui.PlaylistCategory.Global) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Public", style = MaterialTheme.typography.bodyMedium)
                            androidx.compose.material3.Switch(
                                checked = editVisibility == com.androidprj.fuzic.model.ui.PlaylistVisibility.Public,
                                onCheckedChange = { isPublic -> 
                                    editVisibility = if (isPublic) com.androidprj.fuzic.model.ui.PlaylistVisibility.Public else com.androidprj.fuzic.model.ui.PlaylistVisibility.Private
                                }
                            )
                        }
                    }
                    Button(onClick = { coverPicker.launch("image/*") }) {
                        Text("Change Cover Image")
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showEditDialog = false
                        onSaveEdit(editName, editCover, editCategory, editVisibility)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun PlaylistDetailsContent(
    playlist: PlaylistDetails,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRemoveSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            DetailArtwork(
                artworkUrl = playlist.artworkUrl,
                contentDescription = playlist.title,
            )
        }
        item {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item {
            Text(
                text = stringResource(R.string.playlist_details_by, playlist.ownerName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                text = playlist.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (playlist.songs.isNotEmpty()) {
            item {
                Button(
                    onClick = { onPlayAllClick(playlist) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                    Text(stringResource(R.string.action_play))
                }
            }
            items(playlist.songs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onMoreClick = { onSongMoreClick(song) },
                    onRemoveClick = if (playlist.ownerId.isNotEmpty()) { { onRemoveSongClick(song) } } else null
                )
            }
        } else {
            item {
                ScreenMessage(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = stringResource(R.string.playlist_details_empty_title),
                    message = stringResource(R.string.playlist_details_empty_message),
                    fillMaxSize = false,
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Preview(name = "Playlist details - English", showBackground = true)
@Composable
private fun PlaylistDetailsPreview() {
    FuzicTheme {
        PlaylistDetailsScreen(
            uiState = samplePlaylistDetailsUiState(),
            onBackClick = {},
            onPlayAllClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRemoveSongClick = {},
            onSaveEdit = { _, _, _, _ -> },
            onDeletePlaylist = {},
            onRetryClick = {},
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Preview(name = "Playlist details - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistDetailsPersianPreview() {
    FuzicTheme {
        PlaylistDetailsScreen(
            uiState = samplePlaylistDetailsUiState(),
            onBackClick = {},
            onPlayAllClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRemoveSongClick = {},
            onSaveEdit = { _, _, _, _ -> },
            onDeletePlaylist = {},
            onRetryClick = {},
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Preview(name = "Playlist details empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistDetailsEmptyPreview() {
    FuzicTheme {
        PlaylistDetailsScreen(
            uiState = samplePlaylistDetailsUiState().copy(
                playlist = samplePlaylistDetailsUiState().playlist?.copy(songs = emptyList()),
            ),
            onBackClick = {},
            onPlayAllClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRemoveSongClick = {},
            onSaveEdit = { _, _, _, _ -> },
            onDeletePlaylist = {},
            onRetryClick = {},
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Preview(name = "Playlist details loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistDetailsLoadingPreview() {
    FuzicTheme {
        PlaylistDetailsScreen(
            uiState = PlaylistDetailsUiState(isLoading = true),
            onBackClick = {},
            onPlayAllClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRemoveSongClick = {},
            onSaveEdit = { _, _, _, _ -> },
            onDeletePlaylist = {},
            onRetryClick = {},
        )
    }
}

@Composable
private fun samplePlaylistDetailsUiState() = PlaylistDetailsUiState(
    playlist = PlaylistDetails(
        id = "playlist-tehran-drive",
        title = stringResource(R.string.preview_playlist_tehran_drive),
        description = stringResource(R.string.preview_playlist_description),
        ownerName = stringResource(R.string.preview_profile_display_name),
        ownerId = "",
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        songs = listOf(
            SongItem(
                id = "song-tehran-nights",
                title = stringResource(R.string.preview_song_tehran_nights),
                artist = stringResource(R.string.preview_artist_raha_band),
                durationLabel = stringResource(R.string.preview_song_duration_tehran),
                artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
            ),
            SongItem(
                id = "song-midnight-drive",
                title = stringResource(R.string.preview_song_midnight_drive),
                artist = stringResource(R.string.preview_artist_luna_ray),
                durationLabel = stringResource(R.string.preview_song_duration_midnight),
                artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
            ),
        ),
    ),
)
