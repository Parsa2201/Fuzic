package com.androidprj.fuzic.ui.screens.songcollection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.SongCollectionUiState
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailLoadingContent
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SongListItem
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCollectionScreen(
    uiState: SongCollectionUiState,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
    onPlayAllClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onRemoveClick: (SongItem) -> Unit = {},
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    errorTitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailTopAppBar(
            title = uiState.title,
            onBackClick = onBackClick,
        )
        when {
            uiState.isLoading -> DetailLoadingContent()
            uiState.errorMessage != null -> ScreenMessage(
                icon = Icons.Default.ErrorOutline,
                title = errorTitle,
                message = uiState.errorMessage,
                action = {
                    Button(onClick = onRetryClick) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.padding(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.action_retry))
                    }
                },
            )
            uiState.isEmpty -> ScreenMessage(
                icon = emptyIcon,
                title = emptyTitle,
                message = emptyMessage,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        Button(onClick = onPlayAllClick) { Text(stringResource(R.string.collection_play_all)) }
                        Button(onClick = onShuffleClick) { Text(stringResource(R.string.collection_shuffle)) }
                    }
                }
                items(uiState.songs, key = { it.id }) { song ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) onRemoveClick(song)
                            true
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(MaterialTheme.spacing.medium),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_remove),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                    ) {
                        SongListItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            onMoreClick = { onRemoveClick(song) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Song collection - English", showBackground = true)
@Composable
private fun SongCollectionPreview() {
    FuzicTheme {
        SongCollectionScreen(
            uiState = SongCollectionUiState(
                title = stringResource(R.string.liked_songs_title),
                songs = listOf(
                    SongItem(
                        id = "preview-song",
                        title = stringResource(R.string.preview_song_midnight_drive),
                        artist = stringResource(R.string.preview_artist_luna_ray),
                        durationLabel = stringResource(R.string.preview_song_duration_midnight),
                    ),
                ),
            ),
            onBackClick = {},
            onSongClick = {},
            onSongMoreClick = {},
            onRetryClick = {},
            emptyIcon = Icons.Default.Refresh,
            emptyTitle = stringResource(R.string.liked_songs_empty_title),
            emptyMessage = stringResource(R.string.liked_songs_empty_message),
            errorTitle = stringResource(R.string.liked_songs_error_title),
        )
    }
}

@Preview(name = "Song collection loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongCollectionLoadingPreview() {
    FuzicTheme {
        SongCollectionScreen(
            uiState = SongCollectionUiState(title = stringResource(R.string.liked_songs_title), isLoading = true),
            onBackClick = {}, onSongClick = {}, onSongMoreClick = {}, onRetryClick = {},
            emptyIcon = Icons.Default.Refresh, emptyTitle = stringResource(R.string.liked_songs_empty_title),
            emptyMessage = stringResource(R.string.liked_songs_empty_message), errorTitle = stringResource(R.string.liked_songs_error_title),
        )
    }
}

@Preview(name = "Song collection empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongCollectionEmptyPreview() {
    FuzicTheme {
        SongCollectionScreen(
            uiState = SongCollectionUiState(title = stringResource(R.string.liked_songs_title)),
            onBackClick = {}, onSongClick = {}, onSongMoreClick = {}, onRetryClick = {},
            emptyIcon = Icons.Default.Refresh, emptyTitle = stringResource(R.string.liked_songs_empty_title),
            emptyMessage = stringResource(R.string.liked_songs_empty_message), errorTitle = stringResource(R.string.liked_songs_error_title),
        )
    }
}

@Preview(name = "Song collection error - Persian", locale = "fa", showBackground = true)
@Composable
private fun SongCollectionErrorPreview() {
    FuzicTheme {
        SongCollectionScreen(
            uiState = SongCollectionUiState(title = stringResource(R.string.liked_songs_title), errorMessage = stringResource(R.string.liked_songs_error_title)),
            onBackClick = {}, onSongClick = {}, onSongMoreClick = {}, onRetryClick = {},
            emptyIcon = Icons.Default.Refresh, emptyTitle = stringResource(R.string.liked_songs_empty_title),
            emptyMessage = stringResource(R.string.liked_songs_empty_message), errorTitle = stringResource(R.string.liked_songs_error_title),
        )
    }
}
