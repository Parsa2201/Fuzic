package com.androidprj.fuzic.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.ui.components.DetailTopAppBar
import com.androidprj.fuzic.ui.components.ScreenMessage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay

@Composable
fun AddToPlaylistScreen(
    playlists: List<PlaylistItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        DetailTopAppBar(stringResource(R.string.add_to_playlist_title), onBackClick)
        when {
            errorMessage != null -> ScreenMessage(Icons.Default.PlaylistPlay, stringResource(R.string.add_to_playlist_title), errorMessage)
            !isLoading && playlists.isEmpty() -> ScreenMessage(Icons.Default.PlaylistPlay, stringResource(R.string.add_to_playlist_title), stringResource(R.string.add_to_playlist_empty))
            else -> LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.title) },
                        supportingContent = { Text(playlist.subtitle) },
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylistClick(playlist) },
                    )
                }
            }
        }
    }
}
