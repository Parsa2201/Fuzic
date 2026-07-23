package com.androidprj.fuzic.ui.screens.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.ArtistCollectionItem
import com.androidprj.fuzic.model.ui.ArtistCollectionUiState
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun ArtistsRoute(
    uiState: ArtistCollectionUiState,
    onBackClick: () -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onFollowClick: (ArtistCollectionItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArtistsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onArtistClick = onArtistClick,
        onFollowClick = onFollowClick,
        onRetryClick = onRetryClick,
        modifier = modifier,
    )
}

@Composable
fun ArtistsScreen(
    uiState: ArtistCollectionUiState,
    onBackClick: () -> Unit,
    onArtistClick: (ArtistItem) -> Unit,
    onFollowClick: (ArtistCollectionItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(title = stringResource(R.string.artists_title), onBackClick = onBackClick)
        when {
            uiState.isLoading -> ArtistsLoadingContent(Modifier.weight(1f))
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(R.string.artists_error_title),
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(MaterialTheme.spacing.small))
                        Text(stringResource(R.string.action_retry))
                    }
                },
                modifier = Modifier.weight(1f),
            )
            uiState.isEmpty -> ScreenMessage(
                icon = Icons.Default.Person,
                title = stringResource(R.string.artists_empty_title),
                message = stringResource(R.string.artists_empty_message),
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                items(uiState.artists, key = { it.artist.id }) { item ->
                    ArtistCollectionRow(
                        item = item,
                        onArtistClick = { onArtistClick(item.artist) },
                        onFollowClick = { onFollowClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCollectionRow(
    item: ArtistCollectionItem,
    onArtistClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onArtistClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            MusicArtwork(
                artworkUrl = item.artist.avatarUrl,
                fallbackIcon = Icons.Default.Person,
                contentDescription = item.artist.name,
                modifier = Modifier
                    .size(MaterialTheme.spacing.extraLarge)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.followersLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onFollowClick) {
                Text(
                    stringResource(
                        if (item.isFollowing) R.string.action_unfollow else R.string.action_follow,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ArtistsLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fuzicShimmer(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.spacing.extraLarge)
                        .clip(CircleShape),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = MaterialTheme.spacing.medium),
                )
            }
        }
    }
}

@Composable
private fun sampleArtistCollectionState() = ArtistCollectionUiState(
    artists = listOf(
        ArtistCollectionItem(
            artist = ArtistItem(
                id = "raha-band",
                name = stringResource(R.string.preview_artist_raha_band),
                avatarUrl = previewArtworkUri(2),
                monthlyListenersLabel = stringResource(R.string.preview_artist_monthly_listeners),
            ),
            followersLabel = stringResource(R.string.preview_artist_followers_large),
            isFollowing = true,
        ),
        ArtistCollectionItem(
            artist = ArtistItem(
                id = "nika",
                name = stringResource(R.string.preview_artist_nika),
                avatarUrl = previewArtworkUri(3),
            ),
            followersLabel = stringResource(R.string.preview_artist_followers_medium),
        ),
        ArtistCollectionItem(
            artist = ArtistItem(
                id = "midnight",
                name = stringResource(R.string.preview_artist_midnight),
                avatarUrl = previewArtworkUri(4),
            ),
            followersLabel = stringResource(R.string.preview_artist_followers_small),
        ),
    ),
)

@Preview(name = "Artists - English", showBackground = true)
@Composable
private fun ArtistsPreview() {
    FuzicTheme {
        ArtistsScreen(
            uiState = sampleArtistCollectionState(),
            onBackClick = {},
            onArtistClick = {},
            onFollowClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artists - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistsPersianPreview() {
    FuzicTheme {
        ArtistsScreen(
            uiState = sampleArtistCollectionState(),
            onBackClick = {},
            onArtistClick = {},
            onFollowClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artists empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistsEmptyPreview() {
    FuzicTheme {
        ArtistsScreen(
            uiState = ArtistCollectionUiState(),
            onBackClick = {},
            onArtistClick = {},
            onFollowClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artists loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistsLoadingPreview() {
    FuzicTheme {
        ArtistsScreen(
            uiState = ArtistCollectionUiState(isLoading = true),
            onBackClick = {},
            onArtistClick = {},
            onFollowClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Artists error - Persian", locale = "fa", showBackground = true)
@Composable
private fun ArtistsErrorPreview() {
    FuzicTheme {
        ArtistsScreen(
            uiState = ArtistCollectionUiState(
                errorMessage = stringResource(R.string.preview_artists_error_message),
            ),
            onBackClick = {},
            onArtistClick = {},
            onFollowClick = {},
            onRetryClick = {},
        )
    }
}
