package com.androidprj.fuzic.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.androidprj.fuzic.model.SearchFilter
import com.androidprj.fuzic.model.SearchResultItem
import com.androidprj.fuzic.model.SearchUiState
import com.androidprj.fuzic.ui.components.MusicArtwork
import com.androidprj.fuzic.ui.components.SectionHeader
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing

@Composable
fun SearchRoute(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onFilterClick: (SearchFilter) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryDeleteClick: (String) -> Unit,
    onClearHistoryClick: () -> Unit,
    onResultClick: (SearchResultItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchScreen(
        uiState = uiState,
        onQueryChange = onQueryChange,
        onFilterClick = onFilterClick,
        onHistoryClick = onHistoryClick,
        onHistoryDeleteClick = onHistoryDeleteClick,
        onClearHistoryClick = onClearHistoryClick,
        onResultClick = onResultClick,
        onRetryClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onFilterClick: (SearchFilter) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryDeleteClick: (String) -> Unit,
    onClearHistoryClick: () -> Unit,
    onResultClick: (SearchResultItem) -> Unit,
    onRetryClick: () -> Unit,
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
            SearchInput(
                query = uiState.query,
                onQueryChange = onQueryChange
            )
        }
        item {
            SearchFilterRow(
                filters = uiState.filters,
                selectedFilter = uiState.selectedFilter,
                onFilterClick = onFilterClick
            )
        }
        when {
            uiState.isLoading -> item { SearchLoadingContent() }
            uiState.errorMessage != null -> item {
                SearchMessageContent(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.search_error_title),
                    message = uiState.errorMessage,
                    action = {
                        Button(onClick = onRetryClick) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(MaterialTheme.spacing.small))
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                )
            }
            uiState.shouldShowHistory -> item {
                SearchHistorySection(
                    history = uiState.history,
                    onHistoryClick = onHistoryClick,
                    onHistoryDeleteClick = onHistoryDeleteClick,
                    onClearHistoryClick = onClearHistoryClick
                )
            }
            uiState.shouldShowEmptyQuery -> item {
                SearchMessageContent(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.search_empty_query_title),
                    message = stringResource(R.string.search_empty_query_message)
                )
            }
            uiState.shouldShowNoResults -> item {
                SearchMessageContent(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.search_no_results_title),
                    message = stringResource(R.string.search_no_results_message)
                )
            }
            else -> items(uiState.results) { result ->
                SearchResultRow(
                    item = result,
                    onClick = { onResultClick(result) }
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        label = {
            Text(stringResource(R.string.search_input_label))
        },
        placeholder = {
            Text(stringResource(R.string.search_input_placeholder))
        }
    )
}

@Composable
private fun SearchFilterRow(
    filters: List<SearchFilter>,
    selectedFilter: SearchFilter,
    onFilterClick: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterClick(filter) },
                label = { Text(stringResource(filter.labelRes)) },
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(SearchSizes.FilterIconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryDeleteClick: (String) -> Unit,
    onClearHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(titleRes = R.string.search_history_title, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearHistoryClick) {
                Text(stringResource(R.string.search_history_clear))
            }
        }
        history.forEach { query ->
            SearchHistoryChip(
                query = query,
                onClick = { onHistoryClick(query) },
                onDeleteClick = { onHistoryDeleteClick(query) }
            )
        }
    }
}

@Composable
private fun SearchHistoryChip(
    query: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = SearchSizes.HistoryChipBorderWidth,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .height(SearchSizes.HistoryChipHeight)
                .padding(horizontal = MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SearchSizes.HistoryIconSize)
            )
            Text(
                text = query,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.action_remove),
                modifier = Modifier.clickable(onClick = onDeleteClick),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                fallbackIcon = item.type.icon,
                contentDescription = item.title,
                modifier = Modifier
                    .size(SearchSizes.ResultArtworkSize)
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
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(
                    width = SearchSizes.ResultBadgeBorderWidth,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = stringResource(item.type.labelRes),
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.extraSmall
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SearchLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        repeat(5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(SearchSizes.ResultArtworkSize)
                        .fuzicShimmer(MaterialTheme.shapes.medium)
                )
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Box(
                        modifier = Modifier
                            .width(SearchSizes.TitleSkeletonWidth)
                            .height(SearchSizes.TextSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .width(SearchSizes.SubtitleSkeletonWidth)
                            .height(SearchSizes.TextSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchMessageContent(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(SearchSizes.MessageIconContainerSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.invoke()
        }
    }
}

private val SearchFilter.icon: ImageVector
    get() = when (this) {
        SearchFilter.Songs -> Icons.Default.MusicNote
        SearchFilter.Artists -> Icons.Default.Mic
        SearchFilter.Playlists -> Icons.AutoMirrored.Filled.PlaylistPlay
        SearchFilter.Users -> Icons.Default.Person
    }

private object SearchSizes {
    val FilterIconSize = 18.dp
    val HistoryChipHeight = 64.dp
    val HistoryChipBorderWidth = 1.dp
    val HistoryIconSize = 32.dp
    val ResultBadgeBorderWidth = 1.dp
    val ResultArtworkSize = 56.dp
    val MessageIconContainerSize = 72.dp
    val TitleSkeletonWidth = 180.dp
    val SubtitleSkeletonWidth = 128.dp
    val TextSkeletonHeight = 16.dp
}

@Preview(name = "Search content - English", showBackground = true)
@Composable
private fun SearchScreenContentPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = sampleSearchUiState())
    }
}

@Preview(name = "Search content - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenContentPersianPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = sampleSearchUiState())
    }
}

@Preview(name = "Search history - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenHistoryPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = sampleSearchUiState().copy(query = "", results = emptyList()))
    }
}

@Preview(name = "Search loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenLoadingPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = SearchUiState(query = stringResource(R.string.preview_search_query), isLoading = true))
    }
}

@Preview(name = "Search empty query - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenEmptyQueryPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = SearchUiState())
    }
}

@Preview(name = "Search no results - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenNoResultsPreview() {
    FuzicTheme {
        SearchPreviewState(uiState = SearchUiState(query = stringResource(R.string.preview_search_query)))
    }
}

@Preview(name = "Search error - Persian", locale = "fa", showBackground = true)
@Composable
private fun SearchScreenErrorPreview() {
    FuzicTheme {
        SearchPreviewState(
            uiState = SearchUiState(
                query = stringResource(R.string.preview_search_query),
                errorMessage = stringResource(R.string.search_error_message)
            )
        )
    }
}

@Preview(name = "Search input", showBackground = true)
@Composable
private fun SearchInputPreview() {
    FuzicTheme {
        var query by remember { mutableStateOf("") }
        SearchInput(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Search filters", showBackground = true)
@Composable
private fun SearchFilterRowPreview() {
    FuzicTheme {
        var filter by remember { mutableStateOf(SearchFilter.Songs) }
        SearchFilterRow(
            filters = SearchFilter.entries,
            selectedFilter = filter,
            onFilterClick = { filter = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Search history section", showBackground = true)
@Composable
private fun SearchHistorySectionPreview() {
    FuzicTheme {
        var selectedQuery by remember { mutableStateOf("") }
        SearchHistorySection(
            history = sampleSearchHistory(),
            onHistoryClick = { selectedQuery = it },
            onHistoryDeleteClick = { selectedQuery = it },
            onClearHistoryClick = { selectedQuery = "" },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Search history chip", showBackground = true)
@Composable
private fun SearchHistoryChipPreview() {
    FuzicTheme {
        var selectedQuery by remember { mutableStateOf("") }
        SearchHistoryChip(
            query = stringResource(R.string.preview_search_query),
            onClick = { selectedQuery = "selected" },
            onDeleteClick = { selectedQuery = "" },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Search result row", showBackground = true)
@Composable
private fun SearchResultRowPreview() {
    FuzicTheme {
        val result = sampleSearchResults().first()
        var selectedResult by remember { mutableStateOf(result) }
        SearchResultRow(
            item = result,
            onClick = { selectedResult = result },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(name = "Search loading content", showBackground = true)
@Composable
private fun SearchLoadingContentPreview() {
    FuzicTheme {
        SearchLoadingContent(modifier = Modifier.padding(MaterialTheme.spacing.medium))
    }
}

@Preview(name = "Search message content", showBackground = true)
@Composable
private fun SearchMessageContentPreview() {
    FuzicTheme {
        SearchMessageContent(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_empty_query_title),
            message = stringResource(R.string.search_empty_query_message)
        )
    }
}

@Composable
private fun SearchPreviewState(uiState: SearchUiState) {
    var state by remember { mutableStateOf(uiState) }
    var selectedResult by remember { mutableStateOf(uiState.results.firstOrNull()) }
    SearchScreen(
        uiState = state,
        onQueryChange = { state = state.copy(query = it) },
        onFilterClick = { state = state.copy(selectedFilter = it) },
        onHistoryClick = { state = state.copy(query = it) },
        onHistoryDeleteClick = { query -> state = state.copy(history = state.history - query) },
        onClearHistoryClick = { state = state.copy(history = emptyList()) },
        onResultClick = { selectedResult = it },
        onRetryClick = { state = state.copy(errorMessage = null) }
    )
}

@Composable
private fun sampleSearchUiState() = SearchUiState(
    query = stringResource(R.string.preview_search_query),
    selectedFilter = SearchFilter.Songs,
    history = sampleSearchHistory(),
    results = sampleSearchResults()
)

@Composable
private fun sampleSearchHistory() = listOf(
    stringResource(R.string.preview_search_history_one),
    stringResource(R.string.preview_search_history_two),
    stringResource(R.string.preview_search_history_three)
)

@Composable
private fun sampleSearchResults() = listOf(
    SearchResultItem(
        id = "song-1",
        title = stringResource(R.string.preview_song_midnight_drive),
        subtitle = stringResource(R.string.preview_artist_luna_ray),
        type = SearchFilter.Songs,
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
    ),
    SearchResultItem(
        id = "artist-1",
        title = stringResource(R.string.preview_artist_raha_band),
        subtitle = stringResource(R.string.search_filter_artists),
        type = SearchFilter.Artists,
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
    ),
    SearchResultItem(
        id = "playlist-1",
        title = stringResource(R.string.preview_daily_local_pulse),
        subtitle = stringResource(R.string.search_filter_playlists),
        type = SearchFilter.Playlists,
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_pulse)
    ),
    SearchResultItem(
        id = "user-1",
        title = stringResource(R.string.preview_user_parsa),
        subtitle = stringResource(R.string.search_filter_users),
        type = SearchFilter.Users,
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_echoes)
    )
)

@Composable
private fun previewArtworkUri(resourceId: Int): String {
    val packageName = androidx.compose.ui.platform.LocalContext.current.packageName
    return "android.resource://$packageName/$resourceId"
}
