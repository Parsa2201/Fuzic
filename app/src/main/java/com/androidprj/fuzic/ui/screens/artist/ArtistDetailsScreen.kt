package com.androidprj.fuzic.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ArtistDetailsUiState
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailLoadingContent
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SongListItem
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun ArtistDetailsRoute(
    uiState: ArtistDetailsUiState,
    onBackClick: () -> Unit,
    onFollowClick: (ArtistItem) -> Unit,
    onPlaySongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArtistDetailsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onFollowClick = onFollowClick,
        onPlaySongClick = onPlaySongClick,
        onSongMoreClick = onSongMoreClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun ArtistDetailsScreen(
    uiState: ArtistDetailsUiState,
    onBackClick: () -> Unit,
    onFollowClick: (ArtistItem) -> Unit,
    onPlaySongClick: (SongItem) -> Unit,
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
            title = stringResource(R.string.artist_details_title),
            onBackClick = onBackClick,
        )
        when {
            uiState.isLoading -> DetailLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.artist_error_title),
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
            uiState.artist == null -> ScreenMessage(
                icon = Icons.Default.Person,
                title = stringResource(R.string.artist_error_title),
                message = stringResource(R.string.artist_empty_message),
            )
            else -> ArtistDetailsContent(
                artist = uiState.artist,
                popularSongs = uiState.popularSongs,
                isFollowing = uiState.isFollowing,
                onFollowClick = onFollowClick,
                onPlaySongClick = onPlaySongClick,
                onSongMoreClick = onSongMoreClick,
            )
        }
    }
}

@Composable
private fun ArtistDetailsContent(
    artist: ArtistItem,
    popularSongs: List<SongItem>,
    isFollowing: Boolean,
    onFollowClick: (ArtistItem) -> Unit,
    onPlaySongClick: (SongItem) -> Unit,
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
            MusicArtwork(
                artworkUrl = artist.avatarUrl,
                fallbackIcon = Icons.Default.Person,
                contentDescription = artist.name,
                modifier = Modifier.size(ArtistDetailSizes.AvatarSize),
            )
        }
        item {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }
        artist.monthlyListenersLabel?.let { listeners ->
            item {
                Text(
                    text = listeners,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            FilledTonalButton(
                onClick = { onFollowClick(artist) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (isFollowing) R.string.action_unfollow else R.string.action_follow,
                    ),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.artist_popular_songs),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (popularSongs.isEmpty()) {
            item {
                ScreenMessage(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.artist_empty_title),
                    message = stringResource(R.string.artist_empty_message),
                    fillMaxSize = false,
                )
            }
        } else {
            items(popularSongs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { onPlaySongClick(song) },
                    onMoreClick = { onSongMoreClick(song) },
                )
            }
        }
    }
}

@Preview(name = "Artist details - English", showBackground = true)
@Composable
private fun ArtistDetailsPreview() {
    FuzicTheme {
        ArtistDetailsScreen(
            uiState = sampleArtistDetailsUiState(),
            onBackClick = {},
            onFollowClick = {},
            onPlaySongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artist details - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistDetailsPersianPreview() {
    FuzicTheme {
        ArtistDetailsScreen(
            uiState = sampleArtistDetailsUiState(),
            onBackClick = {},
            onFollowClick = {},
            onPlaySongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artist details empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistDetailsEmptyPreview() {
    FuzicTheme {
        ArtistDetailsScreen(
            uiState = sampleArtistDetailsUiState().copy(popularSongs = emptyList()),
            onBackClick = {},
            onFollowClick = {},
            onPlaySongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artist details loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistDetailsLoadingPreview() {
    FuzicTheme {
        ArtistDetailsScreen(
            uiState = ArtistDetailsUiState(isLoading = true),
            onBackClick = {},
            onFollowClick = {},
            onPlaySongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
        )
    }
}

@Composable
private fun sampleArtistDetailsUiState() = ArtistDetailsUiState(
    artist = ArtistItem(
        id = "artist-raha-band",
        name = stringResource(R.string.preview_artist_raha_band),
        avatarUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        monthlyListenersLabel = stringResource(R.string.preview_artist_monthly_listeners),
    ),
    popularSongs = listOf(
        SongItem(
            id = "song-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            artist = stringResource(R.string.preview_artist_raha_band),
            durationLabel = stringResource(R.string.preview_song_duration_tehran),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        ),
    ),
)

private object ArtistDetailSizes {
    val AvatarSize = 160.dp
}
