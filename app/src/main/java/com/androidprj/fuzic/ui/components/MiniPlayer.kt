package com.androidprj.fuzic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.MiniPlayerUiState
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun MiniPlayer(
    uiState: MiniPlayerUiState,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onExpandDrag: () -> Unit = {},
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val expandThresholdPx = with(density) { MiniPlayerMotion.ExpandThreshold.toPx() }
    var upwardDragPx by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        upwardDragPx = (upwardDragPx - delta).coerceIn(0f, expandThresholdPx)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = -upwardDragPx }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    val shouldExpand = upwardDragPx >= expandThresholdPx ||
                        velocity <= -MiniPlayerMotion.ExpandVelocityThreshold
                    upwardDragPx = 0f
                    if (shouldExpand) onExpandDrag()
                },
            )
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicArtwork(
                artworkUrl = uiState.artworkUrl,
                fallbackIcon = Icons.Default.Album,
                contentDescription = stringResource(R.string.shell_cd_open_player),
                modifier = artworkModifier.size(48.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.shell_now_playing),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.shell_cd_play_pause)
                )
            }
        }
    }
}

private object MiniPlayerMotion {
    val ExpandThreshold = 72.dp
    const val ExpandVelocityThreshold = 1_000f
}

@Preview(name = "Mini player", showBackground = true)
@Composable
private fun MiniPlayerPreview() {
    FuzicTheme {
        MiniPlayer(
            uiState = MiniPlayerUiState(
                title = stringResource(R.string.preview_song_midnight_drive),
                artist = stringResource(R.string.preview_artist_luna_ray),
                artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
                isPlaying = true
            ),
            onClick = {},
            onPlayPauseClick = {}
        )
    }
}

@Preview(name = "Mini player - Persian", locale = "fa", showBackground = true)
@Composable
private fun MiniPlayerPersianPreview() {
    FuzicTheme {
        MiniPlayer(
            uiState = MiniPlayerUiState(
                title = stringResource(R.string.preview_song_tehran_nights),
                artist = stringResource(R.string.preview_artist_raha_band),
                artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
                isPlaying = false
            ),
            onClick = {},
            onPlayPauseClick = {}
        )
    }
}
