package com.androidprj.fuzic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun DetailTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
    }
}

@Composable
fun SongListItem(
    song: SongItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAlbum: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        MusicArtwork(
            artworkUrl = song.artworkUrl,
            fallbackIcon = Icons.Default.Album,
            contentDescription = song.title,
            modifier = Modifier.size(ContentDetailSizes.SongArtworkSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.isExplicit) {
                    Spacer(Modifier.width(MaterialTheme.spacing.extraSmall))
                    Icon(
                        imageVector = Icons.Default.Explicit,
                        contentDescription = stringResource(R.string.content_description_explicit),
                        modifier = Modifier.size(ContentDetailSizes.ExplicitIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showAlbum && song.album != null) {
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        song.durationLabel?.let { duration ->
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.content_description_song_options),
            )
        }
    }
}

@Composable
fun DetailArtwork(
    artworkUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    MusicArtwork(
        artworkUrl = artworkUrl,
        fallbackIcon = Icons.Default.Album,
        contentDescription = contentDescription,
        modifier = modifier.size(ContentDetailSizes.DetailArtworkSize),
    )
}

@Composable
fun DetailLoadingContent(
    modifier: Modifier = Modifier,
    artworkSize: androidx.compose.ui.unit.Dp = ContentDetailSizes.DetailArtworkSize,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Spacer(
            modifier = Modifier
                .size(artworkSize)
                .fuzicShimmer(),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(ContentDetailSizes.TitlePlaceholderHeight)
                .fuzicShimmer(),
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth(ContentDetailSizes.SubtitlePlaceholderFraction)
                .height(ContentDetailSizes.TextPlaceholderHeight)
                .fuzicShimmer(),
        )
    }
}

private object ContentDetailSizes {
    val SongArtworkSize = 56.dp
    val DetailArtworkSize = 224.dp
    val ExplicitIconSize = 16.dp
    val TitlePlaceholderHeight = 32.dp
    val TextPlaceholderHeight = 16.dp
    const val SubtitlePlaceholderFraction = 0.6f
}
