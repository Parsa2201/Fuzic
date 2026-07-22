package com.androidprj.fuzic.ui.screens.player

import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.PlayerOverlay
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.components.DetailArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SongListItem
import com.androidprj.fuzic.ui.components.fuzicClickable
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun PlayerRoute(
    uiState: PlayerUiState,
    onCloseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onQueueSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onOverlayDismiss: () -> Unit,
    onSleepTimerSelected: (Int?) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onPlaylistSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    PlayerScreen(
        uiState = uiState,
        onCloseClick = onCloseClick,
        onPreviousClick = onPreviousClick,
        onPlayPauseClick = onPlayPauseClick,
        onNextClick = onNextClick,
        onSeek = onSeek,
        onShuffleClick = onShuffleClick,
        onRepeatClick = onRepeatClick,
        onLikeClick = onLikeClick,
        onShareClick = onShareClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onQueueClick = onQueueClick,
        onSleepTimerClick = onSleepTimerClick,
        onPlaybackSpeedClick = onPlaybackSpeedClick,
        onQueueSongClick = onQueueSongClick,
        onSongMoreClick = onSongMoreClick,
        onOverlayDismiss = onOverlayDismiss,
        onSleepTimerSelected = onSleepTimerSelected,
        onPlaybackSpeedSelected = onPlaybackSpeedSelected,
        onPlaylistSelected = onPlaylistSelected,
        modifier = modifier,
        artworkModifier = artworkModifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onCloseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onQueueSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
    onOverlayDismiss: () -> Unit,
    onSleepTimerSelected: (Int?) -> Unit,
    onPlaybackSpeedSelected: (Float) -> Unit,
    onPlaylistSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            uiState.errorMessage != null -> PlayerMessage(
                title = stringResource(R.string.player_error_title),
                message = uiState.errorMessage,
                icon = Icons.Default.ErrorOutline,
            )
            uiState.currentSong == null -> PlayerMessage(
                title = stringResource(R.string.player_empty_title),
                message = stringResource(R.string.player_empty_message),
                icon = Icons.Default.PlayArrow,
            )
            else -> PlayerContent(
                uiState = uiState,
                onCloseClick = onCloseClick,
                onPreviousClick = onPreviousClick,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onSeek = onSeek,
                onShuffleClick = onShuffleClick,
                onRepeatClick = onRepeatClick,
                onLikeClick = onLikeClick,
                onShareClick = onShareClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onQueueClick = onQueueClick,
                onSleepTimerClick = onSleepTimerClick,
                onPlaybackSpeedClick = onPlaybackSpeedClick,
                artworkModifier = artworkModifier,
            )
        }

        when (uiState.selectedOverlay) {
            PlayerOverlay.Queue -> QueueSheet(
                queue = uiState.queue,
                onDismiss = onOverlayDismiss,
                onSongClick = onQueueSongClick,
                onSongMoreClick = onSongMoreClick,
            )
            PlayerOverlay.SleepTimer -> SleepTimerSheet(
                selectedMinutes = uiState.sleepTimerMinutes,
                onDismiss = onOverlayDismiss,
                onSelected = onSleepTimerSelected,
            )
            PlayerOverlay.PlaybackSpeed -> PlaybackSpeedSheet(
                selectedSpeed = uiState.playbackSpeed,
                onDismiss = onOverlayDismiss,
                onSelected = onPlaybackSpeedSelected,
            )
            PlayerOverlay.Share -> ShareSheet(onDismiss = onOverlayDismiss)
            PlayerOverlay.AddToPlaylist -> AddToPlaylistSheet(
                onDismiss = onOverlayDismiss,
                onPlaylistSelected = onPlaylistSelected,
            )
            PlayerOverlay.None -> Unit
        }
    }
}

@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    onCloseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    val song = uiState.currentSong ?: return
    val artworkRotation = rememberArtworkRotation(isPlaying = uiState.isPlaying)
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.player_title),
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.player_close),
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        DetailArtwork(
            artworkUrl = song.artworkUrl,
            contentDescription = song.title,
            modifier = artworkModifier
                .size(PlayerSizes.CoverSize)
                .graphicsLayer { rotationZ = artworkRotation },
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (uiState.isBuffering) {
            Text(
                text = stringResource(R.string.player_reconnecting),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        AudioVisualizer(
            isPlaying = uiState.isPlaying,
            amplitudes = uiState.visualizerAmplitudes,
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerSizes.VisualizerHeight),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Slider(
            value = uiState.progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(uiState.elapsedLabel, style = MaterialTheme.typography.labelMedium)
            Text(uiState.durationLabel, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        PlayerTransportControls(
            uiState = uiState,
            onPreviousClick = onPreviousClick,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onShuffleClick = onShuffleClick,
            onRepeatClick = onRepeatClick,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.large))
        PlayerActionGrid(
            uiState = uiState,
            onLikeClick = onLikeClick,
            onShareClick = onShareClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onQueueClick = onQueueClick,
            onSleepTimerClick = onSleepTimerClick,
            onPlaybackSpeedClick = onPlaybackSpeedClick,
        )
    }
}

@Composable
private fun rememberArtworkRotation(isPlaying: Boolean): Float {
    val rotation = androidx.compose.runtime.remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(
                    targetValue = rotation.value + PlayerSizes.ArtworkRotationDegrees,
                    animationSpec = tween(
                        durationMillis = PlayerSizes.ArtworkRotationDurationMillis,
                        easing = LinearEasing,
                    ),
                )
                rotation.snapTo(rotation.value % PlayerSizes.ArtworkRotationDegrees)
            }
        }
    }
    return rotation.value
}

@Composable
private fun PlayerActionGrid(
    uiState: PlayerUiState,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            MaterialTheme.spacing.medium,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.spacing.small,
            ),
        ) {
            PlayerActionButton(
                icon = if (uiState.isLiked) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                label = stringResource(
                    if (uiState.isLiked) {
                        R.string.player_unlike
                    } else {
                        R.string.player_like
                    },
                ),
                tint = if (uiState.isLiked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                onClick = onLikeClick,
                modifier = Modifier.weight(1f),
            )

            PlayerActionButton(
                icon = Icons.Default.Share,
                label = stringResource(R.string.player_share),
                onClick = onShareClick,
                modifier = Modifier.weight(1f),
            )

            PlayerActionButton(
                icon = Icons.Default.Add,
                label = stringResource(R.string.player_add_to_playlist),
                onClick = onAddToPlaylistClick,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.spacing.small,
            ),
        ) {
            PlayerActionButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.player_queue),
                onClick = onQueueClick,
                modifier = Modifier.weight(1f),
            )

            PlayerActionButton(
                icon = Icons.Default.Timer,
                label = uiState.sleepTimerMinutes?.let { minutes ->
                    stringResource(
                        R.string.player_timer_remaining,
                        minutes,
                    )
                } ?: stringResource(R.string.player_sleep_timer),
                onClick = onSleepTimerClick,
                modifier = Modifier.weight(1f),
            )

            PlayerActionButton(
                icon = Icons.Default.Speed,
                label = stringResource(
                    R.string.player_speed_format,
                    uiState.playbackSpeed,
                ),
                onClick = onPlaybackSpeedClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerTransportControls(
    uiState: PlayerUiState,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffleClick) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = stringResource(
                    if (uiState.isShuffleEnabled) R.string.player_shuffle_on else R.string.player_shuffle_off,
                ),
                tint = if (uiState.isShuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        IconButton(onClick = onNextClick) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                modifier = Modifier.size(PlayerSizes.TransportIconSize),
            )
        }
        Surface(
            modifier = Modifier
                .size(PlayerSizes.PlayButtonSize)
                .fuzicClickable(onPlayPauseClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.shell_cd_play_pause),
                    modifier = Modifier.size(PlayerSizes.PlayIconSize),
                )
            }
        }
        IconButton(onClick = onPreviousClick) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                modifier = Modifier.size(PlayerSizes.TransportIconSize),
            )
        }
        IconButton(onClick = onRepeatClick) {
            Icon(
                imageVector = if (uiState.repeatMode == RepeatMode.One) Icons.Default.Replay else Icons.Default.Repeat,
                contentDescription = when (uiState.repeatMode) {
                    RepeatMode.Off -> stringResource(R.string.player_repeat_off)
                    RepeatMode.All -> stringResource(R.string.player_repeat_all)
                    RepeatMode.One -> stringResource(R.string.player_repeat_one)
                },
                tint = if (uiState.repeatMode == RepeatMode.Off) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .fuzicClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    amplitudes: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "playerVisualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = AnimationRepeatMode.Restart,
        ),
        label = "playerVisualizerPhase",
    )
    val visualizerDescription = stringResource(R.string.player_visualizer_description)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier.semantics {
            contentDescription = visualizerDescription
        },
    ) {
        drawVisualizer(
            phase = if (isPlaying) phase else 0.15f,
            amplitudes = amplitudes,
            color = primaryColor,
            secondaryColor = secondaryColor,
        )
    }
}

private fun DrawScope.drawVisualizer(
    phase: Float,
    amplitudes: List<Float>,
    color: Color,
    secondaryColor: Color,
) {
    val barCount =  thirtyTwo
    val gap = size.width / (barCount * 2f)
    val barWidth = gap
    val centerY = size.height / 2f
    repeat(barCount) { index ->
        val normalized = index.toFloat() / barCount
        val signal = amplitudes.getOrNull(index)
        val wave = signal ?: abs(sin((normalized * 9f) + phase * 6.28f))
        val envelope = 0.25f + (1f - abs(normalized - 0.5f) * 1.6f).coerceIn(0f, 1f)
        val barHeight = size.height * (0.18f + wave * envelope * 0.72f)
        val x = gap + index * (barWidth + gap)
        drawLine(
            color = if (index % 3 == 0) secondaryColor else color,
            start = Offset(x, centerY - barHeight / 2f),
            end = Offset(x, centerY + barHeight / 2f),
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<SongItem>,
    onDismiss: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMoreClick: (SongItem) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.player_queue),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (queue.isEmpty()) {
                ScreenMessage(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = stringResource(R.string.player_queue_empty_title),
                    message = stringResource(R.string.player_queue_empty_message),
                    fillMaxSize = false,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium),
                ) {
                    items(queue, key = { it.id }) { song ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    selectedMinutes: Int?,
    onDismiss: () -> Unit,
    onSelected: (Int?) -> Unit,
) {
    val options = listOf(null, 15, 30, 45, 60)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        ) {
            Text(
                text = stringResource(R.string.player_sleep_timer),
                style = MaterialTheme.typography.headlineSmall,
            )
            options.forEach { minutes ->
                ListItem(
                    headlineContent = {
                        Text(
                            minutes?.let {
                                stringResource(R.string.player_timer_minutes, it)
                            } ?: stringResource(R.string.player_timer_off),
                        )
                    },
                    trailingContent = {
                        if (minutes == selectedMinutes) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { onSelected(minutes) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSpeedSheet(
    selectedSpeed: Float,
    onDismiss: () -> Unit,
    onSelected: (Float) -> Unit,
) {
    val options = listOf(1f, 1.5f, 2f)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        ) {
            Text(
                text = stringResource(R.string.player_playback_speed),
                style = MaterialTheme.typography.headlineSmall,
            )
            options.forEach { speed ->
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.player_speed_format, speed))
                    },
                    trailingContent = {
                        if (speed == selectedSpeed) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { onSelected(speed) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = stringResource(R.string.player_share),
                style = MaterialTheme.typography.headlineSmall,
            )
            ListItem(
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.player_share_to_chat)) },
                supportingContent = {
                    Text(stringResource(R.string.player_share_to_chat_description))
                },
                modifier = Modifier.clickable(onClick = onDismiss),
            )
            ListItem(
                leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.player_copy_song_link)) },
                supportingContent = {
                    Text(stringResource(R.string.player_copy_song_link_description))
                },
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit,
) {
    val playlists = listOf(
        stringResource(R.string.preview_playlist_my_night),
        stringResource(R.string.preview_playlist_evening_mix),
        stringResource(R.string.preview_playlist_persian_pulse),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        ) {
            Text(
                text = stringResource(R.string.player_add_to_playlist),
                style = MaterialTheme.typography.headlineSmall,
            )
            playlists.forEach { playlist ->
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                        )
                    },
                    headlineContent = { Text(playlist) },
                    modifier = Modifier.clickable {
                        onPlaylistSelected(playlist)
                        onDismiss()
                    },
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
private fun PlayerMessage(
    title: String,
    message: String,
    icon: ImageVector,
) {
    ScreenMessage(
        icon = icon,
        title = title,
        message = message,
    )
}

@Preview(name = "Full player - playing", showBackground = true)
@Composable
private fun PlayerPlayingPreview() {
    FuzicTheme {
        PlayerScreen(
            uiState = samplePlayerState(isPlaying = true),
            onCloseClick = {},
            onPreviousClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onSeek = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onLikeClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onQueueClick = {},
            onSleepTimerClick = {},
            onPlaybackSpeedClick = {},
            onQueueSongClick = {},
            onSongMoreClick = {},
            onOverlayDismiss = {},
            onSleepTimerSelected = {},
            onPlaybackSpeedSelected = {},
        )
    }
}

@Preview(name = "Full player - paused Persian", locale = "fa", showBackground = true)
@Composable
private fun PlayerPausedPersianPreview() {
    FuzicTheme {
        PlayerScreen(
            uiState = samplePlayerState(isPlaying = false, isLiked = false),
            onCloseClick = {},
            onPreviousClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onSeek = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onLikeClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onQueueClick = {},
            onSleepTimerClick = {},
            onPlaybackSpeedClick = {},
            onQueueSongClick = {},
            onSongMoreClick = {},
            onOverlayDismiss = {},
            onSleepTimerSelected = {},
            onPlaybackSpeedSelected = {},
        )
    }
}

@Preview(name = "Full player - buffering Persian", locale = "fa", showBackground = true)
@Composable
private fun PlayerBufferingPreview() {
    FuzicTheme {
        PlayerScreen(
            uiState = samplePlayerState(isPlaying = true).copy(isBuffering = true),
            onCloseClick = {},
            onPreviousClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onSeek = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onLikeClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onQueueClick = {},
            onSleepTimerClick = {},
            onPlaybackSpeedClick = {},
            onQueueSongClick = {},
            onSongMoreClick = {},
            onOverlayDismiss = {},
            onSleepTimerSelected = {},
            onPlaybackSpeedSelected = {},
        )
    }
}

@Preview(name = "Full player - empty Persian", locale = "fa", showBackground = true)
@Composable
private fun PlayerEmptyPreview() {
    FuzicTheme {
        PlayerScreen(
            uiState = PlayerUiState(),
            onCloseClick = {},
            onPreviousClick = {},
            onPlayPauseClick = {},
            onNextClick = {},
            onSeek = {},
            onShuffleClick = {},
            onRepeatClick = {},
            onLikeClick = {},
            onShareClick = {},
            onAddToPlaylistClick = {},
            onQueueClick = {},
            onSleepTimerClick = {},
            onPlaybackSpeedClick = {},
            onQueueSongClick = {},
            onSongMoreClick = {},
            onOverlayDismiss = {},
            onSleepTimerSelected = {},
            onPlaybackSpeedSelected = {},
        )
    }
}

@Preview(name = "Queue sheet", showBackground = true)
@Composable
private fun QueueSheetPreview() {
    FuzicTheme {
        QueueSheet(
            queue = samplePlayerState().queue,
            onDismiss = {},
            onSongClick = {},
            onSongMoreClick = {},
        )
    }
}

@Preview(name = "Sleep timer sheet - Persian", locale = "fa", showBackground = true)
@Composable
private fun SleepTimerSheetPreview() {
    FuzicTheme {
        SleepTimerSheet(
            selectedMinutes = 30,
            onDismiss = {},
            onSelected = {},
        )
    }
}

@Preview(name = "Playback speed sheet - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaybackSpeedSheetPreview() {
    FuzicTheme {
        PlaybackSpeedSheet(
            selectedSpeed = 1.5f,
            onDismiss = {},
            onSelected = {},
        )
    }
}

@Preview(name = "Share sheet - English", showBackground = true)
@Composable
private fun ShareSheetPreview() {
    FuzicTheme {
        ShareSheet(onDismiss = {})
    }
}

@Preview(name = "Share sheet - Persian", locale = "fa", showBackground = true)
@Composable
private fun ShareSheetPersianPreview() {
    FuzicTheme {
        ShareSheet(onDismiss = {})
    }
}

@Preview(name = "Add to playlist sheet - Persian", locale = "fa", showBackground = true)
@Composable
private fun AddToPlaylistSheetPreview() {
    FuzicTheme {
        AddToPlaylistSheet(
            onDismiss = {},
            onPlaylistSelected = {},
        )
    }
}

@Preview(name = "Visualizer paused", showBackground = true)
@Composable
private fun VisualizerPausedPreview() {
    FuzicTheme {
        AudioVisualizer(
            isPlaying = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerSizes.VisualizerHeight),
        )
    }
}

@Composable
private fun samplePlayerState(
    isPlaying: Boolean = true,
    isLiked: Boolean = true,
) = PlayerUiState(
    currentSong = SongItem(
        id = "song-midnight-drive",
        title = stringResource(R.string.preview_song_midnight_drive),
        artist = stringResource(R.string.preview_artist_luna_ray),
        album = stringResource(R.string.preview_daily_midnight_vinyl),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
        durationLabel = stringResource(R.string.preview_player_duration),
    ),
    isPlaying = isPlaying,
    isLiked = isLiked,
    progress = 0.38f,
    elapsedLabel = stringResource(R.string.preview_player_elapsed),
    durationLabel = stringResource(R.string.preview_player_duration),
    isShuffleEnabled = true,
    repeatMode = com.androidprj.fuzic.model.ui.RepeatMode.All,
    playbackSpeed = 1.5f,
    queue = listOf(
        SongItem(
            id = "song-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            artist = stringResource(R.string.preview_artist_raha_band),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran),
        ),
        SongItem(
            id = "song-golden-echoes",
            title = stringResource(R.string.preview_song_golden_echoes),
            artist = stringResource(R.string.preview_artist_arman),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_echoes),
        ),
    ),
    sleepTimerMinutes = 30,
)

private object PlayerSizes {
    val CoverSize = 240.dp
    val VisualizerHeight = 72.dp
    val PlayButtonSize = 72.dp
    val PlayIconSize = 36.dp
    val TransportIconSize = 32.dp
    const val ArtworkRotationDegrees = 360f
    const val ArtworkRotationDurationMillis = 12_000
}

private const val thirtyTwo = 32
