package com.androidprj.fuzic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun MusicArtwork(
    artworkUrl: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUrl.isNullOrBlank()) {
            ArtworkFallbackIcon(fallbackIcon = fallbackIcon)
        } else {
            SubcomposeAsyncImage(
                model = artworkUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { ArtworkFallbackIcon(fallbackIcon = fallbackIcon) },
                error = { ArtworkFallbackIcon(fallbackIcon = fallbackIcon) },
                success = { SubcomposeAsyncImageContent() }
            )
        }
    }
}

/** Full-player artwork. It shares [MusicArtwork]'s image and fallback behavior. */
@Composable
fun CircularMusicArtwork(
    artworkUrl: String?,
    fallbackIcon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    MusicArtwork(
        artworkUrl = artworkUrl,
        fallbackIcon = fallbackIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = CircleShape,
    )
}

@Composable
private fun ArtworkFallbackIcon(fallbackIcon: ImageVector) {
    Icon(
        imageVector = fallbackIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large)
    )
}

@Preview(name = "Music artwork fallback", showBackground = true)
@Composable
private fun MusicArtworkFallbackPreview() {
    FuzicTheme {
        MusicArtwork(
            artworkUrl = null,
            fallbackIcon = Icons.Default.Album,
            modifier = Modifier.size(120.dp)
        )
    }
}

@Preview(name = "Music artwork URL", showBackground = true)
@Composable
private fun MusicArtworkUrlPreview() {
    FuzicTheme {
        MusicArtwork(
            artworkUrl = "https://img.magnific.com/free-vector/elegant-musical-notes-music-chord-background_1017-20759.jpg?semt=ais_hybrid&w=740&q=80",
            fallbackIcon = Icons.Default.Album,
            modifier = Modifier.size(120.dp)
        )
    }
}

@Preview(name = "Circular music artwork", showBackground = true)
@Composable
private fun CircularMusicArtworkPreview() {
    FuzicTheme {
        CircularMusicArtwork(
            artworkUrl = null,
            fallbackIcon = Icons.Default.Album,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
    }
}
