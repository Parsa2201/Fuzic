package com.androidprj.fuzic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.theme.FuzicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheet(
    song: SongItem,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text(song.artist) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_play)) },
                leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPlayClick),
            )
            if (onAddToPlaylistClick != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_add_to_playlist)) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAddToPlaylistClick),
                )
            }
            if (onRemoveFromPlaylistClick != null) {
                ListItem(
                    headlineContent = { Text("Remove from playlist") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onRemoveFromPlaylistClick),
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.player_share_to_chat)) },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onShareClick),
            )
        }
    }
}

@Preview(name = "Song actions - English", showBackground = true)
@Preview(name = "Song actions - Persian", locale = "fa", showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongActionSheetPreview() {
    FuzicTheme {
        SongActionSheet(
            song = SongItem("preview-song", stringResource(R.string.preview_song_midnight_drive), stringResource(R.string.preview_artist_luna_ray)),
            onDismiss = {}, onPlayClick = {}, onAddToPlaylistClick = {}, onShareClick = {},
        )
    }
}
