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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

@Composable
fun PlaylistDetailsRoute(
    uiState: PlaylistDetailsUiState,
    onBackClick: () -> Unit,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaylistDetailsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPlayAllClick = onPlayAllClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun PlaylistDetailsScreen(
    uiState: PlaylistDetailsUiState,
    onBackClick: () -> Unit,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(
            title = stringResource(R.string.playlist_details_title),
            onBackClick = onBackClick,
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
            )
        }
    }
}

@Composable
private fun PlaylistDetailsContent(
    playlist: PlaylistDetails,
    onPlayAllClick: (PlaylistDetails) -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
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
            onRetryClick = {},
        )
    }
}

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
            onRetryClick = {},
        )
    }
}

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
            onRetryClick = {},
        )
    }
}

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
