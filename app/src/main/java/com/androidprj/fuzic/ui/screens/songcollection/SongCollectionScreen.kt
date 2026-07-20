package com.androidprj.fuzic.ui.screens.songcollection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.SongCollectionUiState
import com.androidprj.fuzic.model.SongItem
import com.androidprj.fuzic.ui.components.DetailLoadingContent
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SongListItem
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun SongCollectionScreen(
    uiState: SongCollectionUiState,
    onBackClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onRetryClick: () -> Unit,
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
                items(uiState.songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMoreClick = { onSongMoreClick(song) },
                    )
                }
            }
        }
    }
}
