package com.androidprj.fuzic.ui.screens.liked

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.SongCollectionUiState
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.screens.songcollection.SongCollectionScreen

@Composable
fun LikedSongsRoute(
    uiState: SongCollectionUiState,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LikedSongsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun LikedSongsScreen(
    uiState: SongCollectionUiState,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SongCollectionScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRetryClick = onRetryClick,
        emptyIcon = Icons.Default.Favorite,
        emptyTitle = stringResource(R.string.liked_songs_empty_title),
        emptyMessage = stringResource(R.string.liked_songs_empty_message),
        errorTitle = stringResource(R.string.liked_songs_error_title),
        modifier = modifier,
    )
}

@Preview(name = "Liked songs - English", showBackground = true)
@Composable
private fun LikedSongsPreview() {
    FuzicTheme {
        LikedSongsScreen(
            uiState = sampleLikedSongsUiState(),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Liked songs empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun LikedSongsEmptyPreview() {
    FuzicTheme {
        LikedSongsScreen(
            uiState = SongCollectionUiState(title = stringResource(R.string.liked_songs_title)),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Liked songs loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun LikedSongsLoadingPreview() {
    FuzicTheme {
        LikedSongsScreen(
            uiState = SongCollectionUiState(
                title = stringResource(R.string.liked_songs_title),
                isLoading = true,
            ),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Liked songs error - Persian", locale = "fa", showBackground = true)
@Composable
private fun LikedSongsErrorPreview() {
    FuzicTheme {
        LikedSongsScreen(
            uiState = SongCollectionUiState(
                title = stringResource(R.string.liked_songs_title),
                errorMessage = stringResource(R.string.liked_songs_error_title),
            ),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
private fun sampleLikedSongsUiState() = SongCollectionUiState(
    title = stringResource(R.string.liked_songs_title),
    songs = listOf(
        SongItem(
            id = "song-midnight-drive",
            title = stringResource(R.string.preview_song_midnight_drive),
            artist = stringResource(R.string.preview_artist_luna_ray),
            durationLabel = stringResource(R.string.preview_song_duration_midnight),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
        ),
    ),
)
