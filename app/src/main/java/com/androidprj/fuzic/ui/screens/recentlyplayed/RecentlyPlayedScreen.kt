package com.androidprj.fuzic.ui.screens.recentlyplayed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.SongCollectionUiState
import com.androidprj.fuzic.model.SongItem
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.screens.songcollection.SongCollectionScreen
import com.androidprj.fuzic.ui.theme.FuzicTheme

@Composable
fun RecentlyPlayedRoute(
    uiState: SongCollectionUiState,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecentlyPlayedScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun RecentlyPlayedScreen(
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
        emptyIcon = Icons.Default.History,
        emptyTitle = stringResource(R.string.recently_played_empty_title),
        emptyMessage = stringResource(R.string.recently_played_empty_message),
        errorTitle = stringResource(R.string.recently_played_error_title),
        modifier = modifier,
    )
}

@Preview(name = "Recently played - English", showBackground = true)
@Composable
private fun RecentlyPlayedPreview() {
    FuzicTheme {
        RecentlyPlayedScreen(
            uiState = sampleRecentlyPlayedUiState(),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Recently played empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun RecentlyPlayedEmptyPreview() {
    FuzicTheme {
        RecentlyPlayedScreen(
            uiState = SongCollectionUiState(title = stringResource(R.string.recently_played_title)),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Recently played loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun RecentlyPlayedLoadingPreview() {
    FuzicTheme {
        RecentlyPlayedScreen(
            uiState = SongCollectionUiState(
                title = stringResource(R.string.recently_played_title),
                isLoading = true,
            ),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
private fun sampleRecentlyPlayedUiState() = SongCollectionUiState(
    title = stringResource(R.string.recently_played_title),
    songs = listOf(
        SongItem(
            id = "song-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            artist = stringResource(R.string.preview_artist_raha_band),
            durationLabel = stringResource(R.string.preview_song_duration_tehran),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        ),
        SongItem(
            id = "song-golden-echoes",
            title = stringResource(R.string.preview_song_golden_echoes),
            artist = stringResource(R.string.preview_artist_arman),
            durationLabel = stringResource(R.string.preview_song_duration_echoes),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_echoes),
        ),
    ),
)
