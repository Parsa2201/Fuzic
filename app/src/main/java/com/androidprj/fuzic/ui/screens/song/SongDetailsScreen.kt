package com.androidprj.fuzic.ui.screens.song

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.androidprj.fuzic.model.ui.SongDetailsUiState
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailArtwork
import com.androidprj.fuzic.ui.components.DetailLoadingContent
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun SongDetailsRoute(
    uiState: SongDetailsUiState,
    onBackClick: () -> Unit,
    onPlayClick: (SongItem) -> Unit,
    onLikeClick: (SongItem) -> Unit,
    onDownloadClick: (SongItem) -> Unit,
    onShareClick: (SongItem) -> Unit,
    onAddToPlaylistClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SongDetailsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPlayClick = onPlayClick,
        onLikeClick = onLikeClick,
        onDownloadClick = onDownloadClick,
        onShareClick = onShareClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun SongDetailsScreen(
    uiState: SongDetailsUiState,
    onBackClick: () -> Unit,
    onPlayClick: (SongItem) -> Unit,
    onLikeClick: (SongItem) -> Unit,
    onDownloadClick: (SongItem) -> Unit,
    onShareClick: (SongItem) -> Unit,
    onAddToPlaylistClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(
            title = stringResource(R.string.song_details_title),
            onBackClick = onBackClick,
        )
        when {
            uiState.isLoading -> DetailLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.song_details_error_title),
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
            uiState.song == null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.song_details_empty_title),
                message = stringResource(R.string.song_details_empty_message),
            )
            else -> SongDetailsContent(
                song = uiState.song,
                isLiked = uiState.isLiked,
                isPremiumUser = uiState.isPremiumUser,
                actionErrorMessage = uiState.actionErrorMessage,
                onPlayClick = onPlayClick,
                onLikeClick = onLikeClick,
                onDownloadClick = onDownloadClick,
                onShareClick = onShareClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
            )
        }
    }
}

@Composable
private fun SongDetailsContent(
    song: SongItem,
    isLiked: Boolean,
    isPremiumUser: Boolean,
    actionErrorMessage: String?,
    onPlayClick: (SongItem) -> Unit,
    onLikeClick: (SongItem) -> Unit,
    onDownloadClick: (SongItem) -> Unit,
    onShareClick: (SongItem) -> Unit,
    onAddToPlaylistClick: (SongItem) -> Unit,
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
                artworkUrl = song.artworkUrl,
                contentDescription = song.title,
            )
        }
        item {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        song.album?.let { album ->
            item {
                Text(
                    text = album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        actionErrorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            Button(
                onClick = { onPlayClick(song) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                Text(stringResource(R.string.action_play))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onLikeClick(song) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                IconButton(onClick = { onDownloadClick(song) }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.action_download),
                    )
                }
                IconButton(onClick = { onShareClick(song) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.action_share),
                    )
                }
                IconButton(onClick = { onAddToPlaylistClick(song) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_add_to_playlist),
                    )
                }
            }
        }
        if (!isPremiumUser) {
            item {
                FilledTonalButton(
                    onClick = { onDownloadClick(song) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.song_details_download_premium))
                }
            }
        }
        item {
            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))
        }
    }
}

@Preview(name = "Song details - English", showBackground = true)
@Composable
private fun SongDetailsPreview() {
    FuzicTheme {
        SongDetailsScreen(
            uiState = sampleSongDetailsUiState(),
            onBackClick = {},
            onPlayClick = {},
            onLikeClick = {},
            onDownloadClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Song details - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongDetailsPersianPreview() {
    FuzicTheme {
        SongDetailsScreen(
            uiState = sampleSongDetailsUiState(),
            onBackClick = {},
            onPlayClick = {},
            onLikeClick = {},
            onDownloadClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Song details loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongDetailsLoadingPreview() {
    FuzicTheme {
        SongDetailsScreen(
            uiState = SongDetailsUiState(isLoading = true),
            onBackClick = {},
            onPlayClick = {},
            onLikeClick = {},
            onDownloadClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Song details error - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongDetailsErrorPreview() {
    FuzicTheme {
        SongDetailsScreen(
            uiState = SongDetailsUiState(errorMessage = stringResource(R.string.song_details_error_title)),
            onBackClick = {},
            onPlayClick = {},
            onLikeClick = {},
            onDownloadClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
private fun sampleSongDetailsUiState() = SongDetailsUiState(
    song = SongItem(
        id = "song-midnight-drive",
        title = stringResource(R.string.preview_song_midnight_drive),
        artist = stringResource(R.string.preview_artist_luna_ray),
        album = stringResource(R.string.preview_daily_midnight_vinyl),
        durationLabel = stringResource(R.string.preview_song_duration_midnight),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
    ),
    isLiked = true,
)
