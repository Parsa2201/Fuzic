package com.androidprj.fuzic.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.CreatePlaylistUiState
import com.androidprj.fuzic.model.ui.PlaylistGradient
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.PlaylistSection
import com.androidprj.fuzic.model.ui.PlaylistSectionType
import com.androidprj.fuzic.model.ui.PlaylistsUiState
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.ScreenMessage
import com.androidprj.fuzic.ui.components.SectionHeader
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun PlaylistsRoute(
    uiState: PlaylistsUiState,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onNewPlaylistClick: () -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreateCoverSelected: (String?) -> Unit,
    onCreateConfirmClick: () -> Unit,
    onCreateDismissClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaylistsScreen(
        uiState = uiState,
        onPlaylistClick = onPlaylistClick,
        onNewPlaylistClick = onNewPlaylistClick,
        onCreateNameChange = onCreateNameChange,
        onCreateCoverSelected = onCreateCoverSelected,
        onCreateConfirmClick = onCreateConfirmClick,
        onCreateDismissClick = onCreateDismissClick,
        onRetryClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
fun PlaylistsScreen(
    uiState: PlaylistsUiState,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onNewPlaylistClick: () -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreateCoverSelected: (String?) -> Unit,
    onCreateConfirmClick: () -> Unit,
    onCreateDismissClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> PlaylistsLoadingContent(modifier)
        uiState.errorMessage != null -> ScreenMessage(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.playlists_error_title),
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
        uiState.isEmpty -> ScreenMessage(
            icon = Icons.Default.LibraryMusic,
            title = stringResource(R.string.playlists_empty_title),
            message = stringResource(R.string.playlists_empty_message),
            action = {
                Button(onClick = onNewPlaylistClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.spacing.small))
                    Text(stringResource(R.string.playlists_new_playlist))
                }
            },
            modifier = modifier
        )
        else -> PlaylistsContent(
            uiState = uiState,
            onPlaylistClick = onPlaylistClick,
            onNewPlaylistClick = onNewPlaylistClick,
            onCreateNameChange = onCreateNameChange,
            onCreateCoverSelected = onCreateCoverSelected,
            onCreateConfirmClick = onCreateConfirmClick,
            onCreateDismissClick = onCreateDismissClick,
            modifier = modifier
        )
    }
}

@Composable
private fun PlaylistsContent(
    uiState: PlaylistsUiState,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onNewPlaylistClick: () -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreateCoverSelected: (String?) -> Unit,
    onCreateConfirmClick: () -> Unit,
    onCreateDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        item {
            PlaylistsHeader(onNewPlaylistClick = onNewPlaylistClick)
        }
        if (uiState.createPlaylistState.isVisible) {
            item {
                CreatePlaylistForm(
                    state = uiState.createPlaylistState,
                    onNameChange = onCreateNameChange,
                    onCoverSelected = onCreateCoverSelected,
                    onConfirmClick = onCreateConfirmClick,
                    onDismissClick = onCreateDismissClick
                )
            }
        }
        uiState.sections.forEach { section ->
            item {
                PlaylistSectionGrid(
                    section = section,
                    onPlaylistClick = onPlaylistClick
                )
            }
        }
    }
}

@Composable
private fun PlaylistsHeader(
    onNewPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.playlists_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Button(onClick = onNewPlaylistClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(MaterialTheme.spacing.small))
            Text(stringResource(R.string.playlists_new_playlist))
        }
    }
}

@Composable
private fun CreatePlaylistForm(
    state: CreatePlaylistUiState,
    onNameChange: (String) -> Unit,
    onCoverSelected: (String?) -> Unit,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onCoverSelected(uri?.toString())
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.playlists_create_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.playlists_name_label)) },
                isError = state.hasNameConflict,
                supportingText = {
                    if (state.hasNameConflict) {
                        Text(stringResource(R.string.playlists_name_conflict_message))
                    }
                }
            )
            Text(
                text = stringResource(R.string.playlists_cover_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                CoverOption(
                    uri = null,
                    selected = state.selectedCoverUri == null,
                    onClick = { onCoverSelected(null) },
                )
                state.availableCoverUris.take(4).forEach { uri ->
                    CoverOption(
                        uri = uri,
                        selected = state.selectedCoverUri == uri,
                        onClick = { onCoverSelected(uri) },
                    )
                }
            }
            OutlinedButton(onClick = { coverPicker.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.playlists_pick_cover))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                OutlinedButton(onClick = onDismissClick) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = onConfirmClick) {
                    Text(stringResource(R.string.action_create))
                }
            }
        }
    }
}

@Composable
private fun CoverOption(
    uri: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        if (uri == null) {
            Icon(
                Icons.Default.Image,
                contentDescription = stringResource(R.string.playlists_cover_default),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.small),
            )
        } else {
            MusicArtwork(
                artworkUrl = uri,
                fallbackIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = stringResource(R.string.playlists_cover_option),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlaylistSectionGrid(
    section: PlaylistSection,
    onPlaylistClick: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        SectionHeader(titleRes = section.titleRes)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.height(gridHeightFor(section.playlists.size)),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            items(section.playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    sectionType = section.type,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistItem,
    sectionType: PlaylistSectionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            if (playlist.artworkUrl != null) {
                MusicArtwork(
                    artworkUrl = playlist.artworkUrl,
                    fallbackIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PlaylistGradientArtwork(
                    id = playlist.id,
                    sectionType = sectionType,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.songCountLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistGradientArtwork(
    id: String,
    sectionType: PlaylistSectionType,
    modifier: Modifier = Modifier
) {
    val gradient = playlistGradient(id, sectionType)
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(gradient.colors),
                shape = MaterialTheme.shapes.medium
            )
            .padding(MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
            contentDescription = null,
            tint = gradient.iconTint,
            modifier = Modifier.size(PlaylistsSizes.PlaylistIconSize)
        )
    }
}

@Composable
private fun playlistGradient(id: String, sectionType: PlaylistSectionType): PlaylistGradientSpec {
    val gradient = PlaylistGradient.entries[(id.hashCode().let { if (it < 0) -it else it }) % PlaylistGradient.entries.size]
    return when (gradient) {
        PlaylistGradient.Violet -> PlaylistGradientSpec(
            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        PlaylistGradient.Teal -> PlaylistGradientSpec(
            listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary),
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        PlaylistGradient.Gold -> PlaylistGradientSpec(
            listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary),
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

private data class PlaylistGradientSpec(
    val colors: List<Color>,
    val iconTint: Color
)

@Composable
private fun PlaylistsLoadingContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        item {
            Box(
                modifier = Modifier
                    .width(PlaylistsSizes.TitleSkeletonWidth)
                    .height(PlaylistsSizes.TitleSkeletonHeight)
                    .fuzicShimmer(MaterialTheme.shapes.small)
            )
        }
        items(3) {
            PlaylistGridLoadingSection()
        }
    }
}

@Composable
private fun PlaylistGridLoadingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Box(
            modifier = Modifier
                .width(PlaylistsSizes.SectionSkeletonWidth)
                .height(PlaylistsSizes.TextSkeletonHeight)
                .fuzicShimmer(MaterialTheme.shapes.small)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            repeat(2) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .fuzicShimmer(MaterialTheme.shapes.medium)
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .width(PlaylistsSizes.CardTitleSkeletonWidth)
                            .height(PlaylistsSizes.TextSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

private object PlaylistsSizes {
    val PlaylistIconSize = 56.dp
    val TitleSkeletonWidth = 180.dp
    val TitleSkeletonHeight = 32.dp
    val SectionSkeletonWidth = 160.dp
    val TextSkeletonHeight = 16.dp
    val CardTitleSkeletonWidth = 120.dp
    val GridRowHeight = 218.dp
}

private fun gridHeightFor(itemCount: Int) = PlaylistsSizes.GridRowHeight * ((itemCount + 1) / 2)

@Preview(name = "Playlists content - English", showBackground = true)
@Composable
private fun PlaylistsScreenContentPreview() {
    FuzicTheme {
        PlaylistsPreviewState(uiState = samplePlaylistsUiState())
    }
}

@Preview(name = "Playlists content - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistsScreenContentPersianPreview() {
    FuzicTheme {
        PlaylistsPreviewState(uiState = samplePlaylistsUiState())
    }
}

@Preview(name = "Playlists create form - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistsScreenCreatePreview() {
    FuzicTheme {
        PlaylistsPreviewState(
            uiState = samplePlaylistsUiState().copy(
                createPlaylistState = CreatePlaylistUiState(
                    isVisible = true,
                    name = stringResource(R.string.preview_playlist_new_name),
                    selectedCoverUri = previewArtworkUri(11),
                    availableCoverUris = listOf(
                        previewArtworkUri(10),
                        previewArtworkUri(11),
                        previewArtworkUri(12),
                        previewArtworkUri(13),
                    ),
                )
            )
        )
    }
}

@Preview(name = "Playlists loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistsScreenLoadingPreview() {
    FuzicTheme {
        PlaylistsPreviewState(uiState = PlaylistsUiState(isLoading = true))
    }
}

@Preview(name = "Playlists empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistsScreenEmptyPreview() {
    FuzicTheme {
        PlaylistsPreviewState(uiState = PlaylistsUiState())
    }
}

@Preview(name = "Playlists error - Persian", locale = "fa", showBackground = true)
@Composable
private fun PlaylistsScreenErrorPreview() {
    FuzicTheme {
        PlaylistsPreviewState(
            uiState = PlaylistsUiState(errorMessage = stringResource(R.string.playlists_error_message))
        )
    }
}

@Preview(name = "Playlists header", showBackground = true)
@Composable
private fun PlaylistsHeaderPreview() {
    FuzicTheme {
        PlaylistsHeader(
            onNewPlaylistClick = { },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Create playlist form", showBackground = true)
@Composable
private fun CreatePlaylistFormPreview() {
    FuzicTheme {
        var state by remember {
            mutableStateOf(CreatePlaylistUiState(isVisible = true, name = ""))
        }
        CreatePlaylistForm(
            state = state.copy(
                availableCoverUris = listOf(
                    previewArtworkUri(10),
                    previewArtworkUri(11),
                    previewArtworkUri(12),
                    previewArtworkUri(13),
                ),
            ),
            onNameChange = { state = state.copy(name = it) },
            onCoverSelected = { state = state.copy(selectedCoverUri = it) },
            onConfirmClick = { },
            onDismissClick = { },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Playlist section grid", showBackground = true)
@Composable
private fun PlaylistSectionGridPreview() {
    FuzicTheme {
        val section = samplePlaylistSections().first()
        var selectedPlaylist by remember { mutableStateOf(section.playlists.first()) }
        PlaylistSectionGrid(
            section = section,
            onPlaylistClick = { selectedPlaylist = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Playlist card", showBackground = true)
@Composable
private fun PlaylistCardPreview() {
    FuzicTheme {
        val playlist = samplePlaylistSections().first().playlists.first()
        var selectedPlaylist by remember { mutableStateOf(playlist) }
        PlaylistCard(
            playlist = playlist,
            sectionType = PlaylistSectionType.Global,
            onClick = { selectedPlaylist = playlist },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Playlist gradient artwork", showBackground = true)
@Composable
private fun PlaylistGradientArtworkPreview() {
    FuzicTheme {
        PlaylistGradientArtwork(
            id = "playlist-preview",
            sectionType = PlaylistSectionType.Local,
            modifier = Modifier
                .size(160.dp)
                .padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Playlists loading content", showBackground = true)
@Composable
private fun PlaylistsLoadingContentPreview() {
    FuzicTheme {
        PlaylistsLoadingContent()
    }
}

@Composable
private fun PlaylistsPreviewState(uiState: PlaylistsUiState) {
    var state by remember { mutableStateOf(uiState) }
    var selectedPlaylist by remember { mutableStateOf(uiState.sections.firstOrNull()?.playlists?.firstOrNull()) }
    PlaylistsScreen(
        uiState = state,
        onPlaylistClick = { selectedPlaylist = it },
        onNewPlaylistClick = { state = state.copy(createPlaylistState = state.createPlaylistState.copy(isVisible = true)) },
        onCreateNameChange = { state = state.copy(createPlaylistState = state.createPlaylistState.copy(name = it)) },
        onCreateCoverSelected = { uri ->
            state = state.copy(
                createPlaylistState = state.createPlaylistState.copy(selectedCoverUri = uri),
            )
        },
        onCreateConfirmClick = { state = state.copy(createPlaylistState = CreatePlaylistUiState()) },
        onCreateDismissClick = { state = state.copy(createPlaylistState = CreatePlaylistUiState()) },
        onRetryClick = { state = state.copy(errorMessage = null) }
    )
}

@Composable
private fun samplePlaylistsUiState() = PlaylistsUiState(
    sections = samplePlaylistSections()
)

@Composable
private fun samplePlaylistSections(): List<PlaylistSection> {
    val global = listOf(
        PlaylistItem(
            id = "global-1",
            title = stringResource(R.string.preview_playlist_global_hits),
            subtitle = stringResource(R.string.playlists_section_global),
            songCountLabel = stringResource(R.string.preview_playlist_song_count_large),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
        ),
        PlaylistItem(
            id = "global-2",
            title = stringResource(R.string.preview_playlist_evening_mix),
            subtitle = stringResource(R.string.playlists_section_global),
            songCountLabel = stringResource(R.string.preview_playlist_song_count_medium),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_echoes)
        )
    )
    val local = listOf(
        PlaylistItem(
            id = "local-1",
            title = stringResource(R.string.preview_playlist_tehran_drive),
            subtitle = stringResource(R.string.playlists_section_local),
            songCountLabel = stringResource(R.string.preview_playlist_song_count_medium),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
        ),
        PlaylistItem(
            id = "local-2",
            title = stringResource(R.string.preview_playlist_persian_pulse),
            subtitle = stringResource(R.string.playlists_section_local),
            songCountLabel = stringResource(R.string.preview_playlist_song_count_small)
        )
    )
    val mine = listOf(
        PlaylistItem(
            id = "mine-1",
            title = stringResource(R.string.preview_playlist_my_night),
            subtitle = stringResource(R.string.playlists_section_mine),
            songCountLabel = stringResource(R.string.preview_playlist_song_count_small),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_pulse)
        )
    )
    return listOf(
        PlaylistSection(R.string.playlists_section_global, PlaylistSectionType.Global, global),
        PlaylistSection(R.string.playlists_section_local, PlaylistSectionType.Local, local),
        PlaylistSection(R.string.playlists_section_mine, PlaylistSectionType.Mine, mine)
    )
}
