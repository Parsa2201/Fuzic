package com.androidprj.fuzic.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.model.ui.DownloadsUiState
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import androidx.compose.material.icons.filled.WorkspacePremium
import com.androidprj.fuzic.ui.components.PremiumFeatureList
import com.androidprj.fuzic.ui.components.PremiumHeroCard

@Composable
fun DownloadsRoute(
    uiState: DownloadsUiState,
    onSortClick: (DownloadSortOption) -> Unit,
    onSongClick: (DownloadedSongItem) -> Unit,
    onDeleteClick: (DownloadedSongItem) -> Unit,
    onUndoDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onFreeUpSpaceClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DownloadsScreen(
        uiState = uiState,
        onSortClick = onSortClick,
        onSongClick = onSongClick,
        onDeleteClick = onDeleteClick,
        onUndoDeleteClick = onUndoDeleteClick,
        onRetryClick = onRetryClick,
        onFreeUpSpaceClick = onFreeUpSpaceClick,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier
    )
}

@Composable
fun DownloadsScreen(
    uiState: DownloadsUiState,
    onSortClick: (DownloadSortOption) -> Unit,
    onSongClick: (DownloadedSongItem) -> Unit,
    onDeleteClick: (DownloadedSongItem) -> Unit,
    onUndoDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onFreeUpSpaceClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isPremiumLoading -> DownloadsLoadingContent(modifier)
        !uiState.isPremiumUser -> DownloadsUpgradeContent(
            isUpgrading = uiState.isUpgrading,
            errorMessage = uiState.errorMessage,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
        uiState.isLoading -> DownloadsLoadingContent(modifier)
        uiState.errorMessage != null -> DownloadsMessageContent(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.downloads_error_title),
            message = uiState.errorMessage,
            action = {
                Button(onClick = onRetryClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.action_retry))
                }
            },
            modifier = modifier
        )
        uiState.isEmpty -> DownloadsMessageContent(
            icon = Icons.Default.Download,
            title = stringResource(R.string.downloads_empty_title),
            message = stringResource(R.string.downloads_empty_message),
            modifier = modifier
        )
        else -> DownloadsContent(
            uiState = uiState,
            onSortClick = onSortClick,
            onSongClick = onSongClick,
            onDeleteClick = onDeleteClick,
            onUndoDeleteClick = onUndoDeleteClick,
            onFreeUpSpaceClick = onFreeUpSpaceClick,
            modifier = modifier
        )
    }
}

@Composable
private fun DownloadsContent(
    uiState: DownloadsUiState,
    onSortClick: (DownloadSortOption) -> Unit,
    onSongClick: (DownloadedSongItem) -> Unit,
    onDeleteClick: (DownloadedSongItem) -> Unit,
    onUndoDeleteClick: () -> Unit,
    onFreeUpSpaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            DownloadsHeader(
                sortOption = uiState.sortOption,
                onSortClick = onSortClick
            )
        }
        if (uiState.isStorageFull) {
            item {
                StorageWarningBanner(onFreeUpSpaceClick = onFreeUpSpaceClick)
            }
        }
        items(uiState.downloads, key = { it.id }) { item ->
            DownloadSwipeRow(
                item = item,
                onSongClick = { onSongClick(item) },
                onDeleteClick = { onDeleteClick(item) },
                onUndoDeleteClick = onUndoDeleteClick
            )
        }
    }
}

@Composable
private fun DownloadsHeader(
    sortOption: DownloadSortOption,
    onSortClick: (DownloadSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.downloads_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            DownloadSortOption.entries.forEach { option ->
                FilterChip(
                    selected = option == sortOption,
                    onClick = { onSortClick(option) },
                    label = { Text(stringResource(option.labelRes)) }
                )
            }
        }
    }
}

@Composable
private fun StorageWarningBanner(
    onFreeUpSpaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Icon(Icons.Default.Storage, contentDescription = null)
            Text(
                text = stringResource(R.string.downloads_storage_full_message),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onFreeUpSpaceClick) {
                Text(stringResource(R.string.downloads_free_up_space))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSwipeRow(
    item: DownloadedSongItem,
    onSongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUndoDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDeleteClick()
                onUndoDeleteClick()
                false
            } else {
                true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DeleteBackground()
        },
        modifier = modifier
    ) {
        DownloadSongRow(
            item = item,
            onClick = onSongClick
        )
    }
}

@Composable
private fun DeleteBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.action_remove),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DownloadSongRow(
    item: DownloadedSongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            MusicArtwork(
                artworkUrl = item.artworkUrl,
                fallbackIcon = Icons.Default.Album,
                contentDescription = item.title,
                modifier = Modifier.size(DownloadsSizes.ArtworkSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                if (item.isDownloadInProgress) {
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.downloads_song_metadata,
                            item.fileSizeLabel,
                            item.downloadedAtLabel
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DownloadsUpgradeContent(
    isUpgrading: Boolean,
    errorMessage: String?,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            PremiumHeroCard()
        }
        item {
            PremiumFeatureList()
        }
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
                )
            }
        }
        item {
            Button(
                onClick = onUpgradeClick,
                enabled = !isUpgrading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUpgrading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.premium_upgrade))
                }
            }
        }
    }
}

@Composable
private fun DownloadsLoadingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            Box(
                modifier = Modifier
                    .width(DownloadsSizes.TitleSkeletonWidth)
                    .height(DownloadsSizes.TitleSkeletonHeight)
                    .fuzicShimmer(MaterialTheme.shapes.small)
            )
        }
        items(5) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(DownloadsSizes.ArtworkSize)
                        .fuzicShimmer(MaterialTheme.shapes.medium)
                )
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Box(
                        modifier = Modifier
                            .width(DownloadsSizes.RowTitleSkeletonWidth)
                            .height(DownloadsSizes.TextSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .width(DownloadsSizes.RowSubtitleSkeletonWidth)
                            .height(DownloadsSizes.TextSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsMessageContent(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    ScreenMessage(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier,
        action = action
    )
}

private object DownloadsSizes {
    val ArtworkSize = 64.dp
    val TitleSkeletonWidth = 180.dp
    val TitleSkeletonHeight = 32.dp
    val RowTitleSkeletonWidth = 180.dp
    val RowSubtitleSkeletonWidth = 128.dp
    val TextSkeletonHeight = 16.dp
}

@Preview(name = "Downloads content - English", showBackground = true)
@Composable
private fun DownloadsScreenContentPreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = sampleDownloadsUiState().copy(isPremiumUser = true, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads content - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadsScreenContentPersianPreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = sampleDownloadsUiState().copy(isPremiumUser = true, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads storage full - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadsScreenStorageFullPreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = sampleDownloadsUiState().copy(isStorageFull = true, isPremiumUser = true, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadsScreenLoadingPreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = DownloadsUiState(isLoading = true, isPremiumUser = true, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadsScreenEmptyPreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = DownloadsUiState(isPremiumUser = true, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads error - Persian", locale = "fa", showBackground = true)
@Composable
private fun DownloadsScreenErrorPreview() {
    FuzicTheme {
        DownloadsPreviewState(
            uiState = DownloadsUiState(errorMessage = stringResource(R.string.downloads_error_message), isPremiumUser = true, isPremiumLoading = false)
        )
    }
}

@Preview(name = "Downloads header", showBackground = true)
@Composable
private fun DownloadsHeaderPreview() {
    FuzicTheme {
        var sortOption by remember { mutableStateOf(DownloadSortOption.DateDownloaded) }
        DownloadsHeader(
            sortOption = sortOption,
            onSortClick = { sortOption = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Storage warning banner", showBackground = true)
@Composable
private fun StorageWarningBannerPreview() {
    FuzicTheme {
        StorageWarningBanner(
            onFreeUpSpaceClick = { },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Download song row", showBackground = true)
@Composable
private fun DownloadSongRowPreview() {
    FuzicTheme {
        val item = sampleDownloadedSongs().first()
        var selectedItem by remember { mutableStateOf(item) }
        DownloadSongRow(
            item = item,
            onClick = { selectedItem = item },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Download in progress row", showBackground = true)
@Composable
private fun DownloadSongProgressRowPreview() {
    FuzicTheme {
        val item = sampleDownloadedSongs().last()
        var selectedItem by remember { mutableStateOf(item) }
        DownloadSongRow(
            item = item,
            onClick = { selectedItem = item },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Delete background", showBackground = true)
@Composable
private fun DeleteBackgroundPreview() {
    FuzicTheme {
        DeleteBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(DownloadsSizes.ArtworkSize)
                .padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Downloads upgrade gate", showBackground = true)
@Composable
private fun DownloadsUpgradeGatePreview() {
    FuzicTheme {
        DownloadsPreviewState(uiState = DownloadsUiState(isPremiumUser = false, isPremiumLoading = false))
    }
}

@Preview(name = "Downloads loading content", showBackground = true)
@Composable
private fun DownloadsLoadingContentPreview() {
    FuzicTheme {
        DownloadsLoadingContent()
    }
}

@Preview(name = "Downloads message content", showBackground = true)
@Composable
private fun DownloadsMessageContentPreview() {
    FuzicTheme {
        DownloadsMessageContent(
            icon = Icons.Default.Download,
            title = stringResource(R.string.downloads_empty_title),
            message = stringResource(R.string.downloads_empty_message)
        )
    }
}

@Composable
private fun DownloadsPreviewState(uiState: DownloadsUiState) {
    var state by remember { mutableStateOf(uiState) }
    var selectedItem by remember { mutableStateOf(uiState.downloads.firstOrNull()) }
    var undoRequested by remember { mutableStateOf(false) }
    DownloadsScreen(
        uiState = state,
        onSortClick = { state = state.copy(sortOption = it) },
        onSongClick = { selectedItem = it },
        onDeleteClick = { item -> state = state.copy(downloads = state.downloads - item) },
        onUndoDeleteClick = { undoRequested = true },
        onRetryClick = { state = state.copy(errorMessage = null) },
        onFreeUpSpaceClick = { state = state.copy(isStorageFull = false) },
        onUpgradeClick = { state = state.copy(isUpgrading = true) }
    )
}

@Composable
private fun sampleDownloadsUiState() = DownloadsUiState(
    downloads = sampleDownloadedSongs()
)

@Composable
private fun sampleDownloadedSongs() = listOf(
    DownloadedSongItem(
        id = "download-1",
        title = stringResource(R.string.preview_song_midnight_drive),
        artist = stringResource(R.string.preview_artist_luna_ray),
        fileSizeLabel = stringResource(R.string.preview_download_size_large),
        downloadedAtLabel = stringResource(R.string.preview_download_date_today),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
    ),
    DownloadedSongItem(
        id = "download-2",
        title = stringResource(R.string.preview_song_tehran_nights),
        artist = stringResource(R.string.preview_artist_raha_band),
        fileSizeLabel = stringResource(R.string.preview_download_size_medium),
        downloadedAtLabel = stringResource(R.string.preview_download_date_yesterday),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
    ),
    DownloadedSongItem(
        id = "download-3",
        title = stringResource(R.string.preview_song_golden_echoes),
        artist = stringResource(R.string.preview_artist_arman),
        fileSizeLabel = stringResource(R.string.preview_download_size_small),
        downloadedAtLabel = stringResource(R.string.preview_download_date_last_week),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_echoes)
    ),
    DownloadedSongItem(
        id = "download-4",
        title = stringResource(R.string.preview_song_electric_heart),
        artist = stringResource(R.string.preview_artist_nika),
        fileSizeLabel = stringResource(R.string.preview_download_size_medium),
        downloadedAtLabel = stringResource(R.string.preview_download_date_today),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_pulse),
        isDownloadInProgress = true,
        progress = 0.58f
    )
)
