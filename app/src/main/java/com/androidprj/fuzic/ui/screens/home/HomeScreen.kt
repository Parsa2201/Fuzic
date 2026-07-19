package com.androidprj.fuzic.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.FeaturedMusicItem
import com.androidprj.fuzic.model.HomeMusicSection
import com.androidprj.fuzic.model.HomeQuickAction
import com.androidprj.fuzic.model.HomeUiState
import com.androidprj.fuzic.ui.components.SectionHeader
import com.androidprj.fuzic.ui.components.fuzicShimmer
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    uiState: HomeUiState,
    onDailyPickClick: (FeaturedMusicItem) -> Unit,
    onQuickActionClick: (HomeQuickAction) -> Unit,
    onMusicItemClick: (FeaturedMusicItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeScreen(
        uiState = uiState,
        onDailyPickClick = onDailyPickClick,
        onQuickActionClick = onQuickActionClick,
        onMusicItemClick = onMusicItemClick,
        onRetryClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDailyPickClick: (FeaturedMusicItem) -> Unit,
    onQuickActionClick: (HomeQuickAction) -> Unit,
    onMusicItemClick: (FeaturedMusicItem) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> HomeLoadingContent(modifier)
        uiState.errorMessage != null -> HomeErrorContent(
            message = uiState.errorMessage,
            onRetryClick = onRetryClick,
            modifier = modifier
        )
        uiState.isEmpty -> HomeEmptyContent(modifier)
        else -> HomeContent(
            uiState = uiState,
            onDailyPickClick = onDailyPickClick,
            onQuickActionClick = onQuickActionClick,
            onMusicItemClick = onMusicItemClick,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onDailyPickClick: (FeaturedMusicItem) -> Unit,
    onQuickActionClick: (HomeQuickAction) -> Unit,
    onMusicItemClick: (FeaturedMusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            bottom = MaterialTheme.spacing.extraLarge
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        item {
            if (uiState.isShowingCachedContent) {
                CachedContentBanner()
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
            }
            DailyPicksCarousel(
                items = uiState.dailyPicks,
                onItemClick = onDailyPickClick
            )
        }
        item {
            QuickActionsGrid(
                actions = uiState.quickActions,
                onActionClick = onQuickActionClick
            )
        }
        items(uiState.sections) { section ->
            MusicCarouselSection(
                section = section,
                onItemClick = onMusicItemClick
            )
        }
    }
}

@Preview(name = "Home content body", showBackground = true)
@Composable
private fun HomeContentPreview() {
    FuzicTheme {
        val uiState = sampleHomeUiState()
        var selectedItem by remember { mutableStateOf(uiState.dailyPicks.first()) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeContent(
            uiState = uiState,
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it }
        )
    }
}

@Composable
private fun CachedContentBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.home_cached_content_message),
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(name = "Cached content banner", showBackground = true)
@Composable
private fun CachedContentBannerPreview() {
    FuzicTheme {
        CachedContentBanner(modifier = Modifier.padding(MaterialTheme.spacing.medium))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyPicksCarousel(
    items: List<FeaturedMusicItem>,
    onItemClick: (FeaturedMusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(4_000)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
            }
        }
    }

    Column(modifier = modifier) {
        SectionHeader(titleRes = R.string.home_daily_picks)
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        HorizontalPager(
            state = pagerState,
            pageSpacing = MaterialTheme.spacing.medium
        ) { page ->
            DailyPickCard(
                item = items[page],
                onClick = { onItemClick(items[page]) }
            )
        }
    }
}

@Preview(name = "Daily picks carousel", showBackground = true)
@Composable
private fun DailyPicksCarouselPreview() {
    FuzicTheme {
        val picks = sampleHomeUiState().dailyPicks
        var selectedItem by remember { mutableStateOf(picks.first()) }
        DailyPicksCarousel(
            items = picks,
            onItemClick = { selectedItem = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun DailyPickCard(
    item: FeaturedMusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(MaterialTheme.spacing.large),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(name = "Daily pick card", showBackground = true)
@Composable
private fun DailyPickCardPreview() {
    FuzicTheme {
        val item = sampleHomeUiState().dailyPicks.first()
        var selectedItem by remember { mutableStateOf(item) }
        DailyPickCard(
            item = item,
            onClick = { selectedItem = item },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun QuickActionsGrid(
    actions: List<HomeQuickAction>,
    onActionClick: (HomeQuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowActions.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(name = "Quick actions grid", showBackground = true)
@Composable
private fun QuickActionsGridPreview() {
    FuzicTheme {
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        QuickActionsGrid(
            actions = HomeQuickAction.defaults,
            onActionClick = { selectedAction = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun QuickActionCard(
    action: HomeQuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Quick action card", showBackground = true)
@Composable
private fun QuickActionCardPreview() {
    FuzicTheme {
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        QuickActionCard(
            action = HomeQuickAction.MyPlaylists,
            onClick = { selectedAction = HomeQuickAction.MyPlaylists },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun MusicCarouselSection(
    section: HomeMusicSection,
    onItemClick: (FeaturedMusicItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(titleRes = section.titleRes)
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(end = MaterialTheme.spacing.medium)
        ) {
            items(section.items) { item ->
                MusicCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Preview(name = "Music carousel section", showBackground = true)
@Composable
private fun MusicCarouselSectionPreview() {
    FuzicTheme {
        val section = sampleHomeUiState().sections.first()
        var selectedItem by remember { mutableStateOf(section.items.first()) }
        MusicCarouselSection(
            section = section,
            onItemClick = { selectedItem = it },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun MusicCard(
    item: FeaturedMusicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(HomeSizes.MusicCardWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HomeSizes.MusicCardIconSize)
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Music card", showBackground = true)
@Composable
private fun MusicCardPreview() {
    FuzicTheme {
        val item = sampleHomeItems().first()
        var selectedItem by remember { mutableStateOf(item) }
        MusicCard(
            item = item,
            onClick = { selectedItem = item },
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun HomeLoadingContent(modifier: Modifier = Modifier) {
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
                    .fillMaxWidth()
                    .aspectRatio(1.7f)
                    .fuzicShimmer(MaterialTheme.shapes.large)
            )
        }
        item {
            QuickActionLoadingGrid()
        }
        items(3) {
            CarouselLoadingSection()
        }
    }
}

@Preview(name = "Home loading content", showBackground = true)
@Composable
private fun HomeLoadingContentPreview() {
    FuzicTheme {
        HomeLoadingContent()
    }
}

@Composable
private fun QuickActionLoadingGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(HomeSizes.QuickActionSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.medium)
                    )
                }
            }
        }
    }
}

@Preview(name = "Quick action loading grid", showBackground = true)
@Composable
private fun QuickActionLoadingGridPreview() {
    FuzicTheme {
        QuickActionLoadingGrid()
    }
}

@Composable
private fun CarouselLoadingSection() {
    Column {
        Box(
            modifier = Modifier
                .width(HomeSizes.SectionTitleSkeletonWidth)
                .height(HomeSizes.SectionTitleSkeletonHeight)
                .fuzicShimmer(MaterialTheme.shapes.small)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            items(4) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(HomeSizes.MusicCardWidth)
                            .fuzicShimmer(MaterialTheme.shapes.medium)
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .width(HomeSizes.MusicTitleSkeletonWidth)
                            .height(HomeSizes.MusicTitleSkeletonHeight)
                            .fuzicShimmer(MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

@Preview(name = "Carousel loading section", showBackground = true)
@Composable
private fun CarouselLoadingSectionPreview() {
    FuzicTheme {
        CarouselLoadingSection()
    }
}

@Composable
private fun HomeEmptyContent(modifier: Modifier = Modifier) {
    HomeMessageContent(
        title = stringResource(R.string.home_empty_title),
        message = stringResource(R.string.home_empty_message),
        modifier = modifier
    )
}

@Preview(name = "Home empty content", showBackground = true)
@Composable
private fun HomeEmptyContentPreview() {
    FuzicTheme {
        HomeEmptyContent()
    }
}

@Composable
private fun HomeErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeMessageContent(
        title = stringResource(R.string.home_error_title),
        message = message,
        action = {
            Button(onClick = onRetryClick) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.action_retry))
            }
        },
        modifier = modifier
    )
}

@Preview(name = "Home error content", showBackground = true)
@Composable
private fun HomeErrorContentPreview() {
    FuzicTheme {
        var retryRequested by remember { mutableStateOf(false) }
        HomeErrorContent(
            message = stringResource(R.string.home_error_message),
            onRetryClick = { retryRequested = true }
        )
    }
}

@Composable
private fun HomeMessageContent(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(MaterialTheme.spacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(HomeSizes.MessageIconContainerSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
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

@Preview(name = "Home message content", showBackground = true)
@Composable
private fun HomeMessageContentPreview() {
    FuzicTheme {
        HomeMessageContent(
            title = stringResource(R.string.home_empty_title),
            message = stringResource(R.string.home_empty_message)
        )
    }
}

private val HomeQuickAction.icon: ImageVector
    get() = when (this) {
        HomeQuickAction.LikedSongs -> Icons.Default.Favorite
        HomeQuickAction.RecentlyPlayed -> Icons.Default.History
        HomeQuickAction.MyPlaylists -> Icons.AutoMirrored.Filled.PlaylistPlay
        HomeQuickAction.TopArtists -> Icons.Default.Groups
    }

private object HomeSizes {
    val MusicCardWidth = 150.dp
    val MusicCardIconSize = 40.dp
    val QuickActionSkeletonHeight = 56.dp
    val SectionTitleSkeletonWidth = 180.dp
    val SectionTitleSkeletonHeight = 28.dp
    val MusicTitleSkeletonWidth = 120.dp
    val MusicTitleSkeletonHeight = 16.dp
    val MessageIconContainerSize = 72.dp
}

@Preview(name = "Home content - English", showBackground = true)
@Composable
private fun HomeScreenContentPreview() {
    FuzicTheme {
        val uiState = sampleHomeUiState()
        val sampleItem = uiState.sections.first().items.first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeScreen(
            uiState = uiState,
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { selectedAction = HomeQuickAction.RecentlyPlayed }
        )
    }
}

@Preview(name = "Home content - Persian", locale = "fa", showBackground = true)
@Composable
private fun HomeScreenContentPersianPreview() {
    FuzicTheme {
        val uiState = sampleHomeUiState()
        val sampleItem = uiState.sections.first().items.first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeScreen(
            uiState = uiState,
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { selectedAction = HomeQuickAction.RecentlyPlayed }
        )
    }
}

@Preview(name = "Home cached content - Persian", locale = "fa", showBackground = true)
@Composable
private fun HomeScreenCachedContentPreview() {
    FuzicTheme {
        val uiState = sampleHomeUiState().copy(isShowingCachedContent = true)
        val sampleItem = uiState.sections.first().items.first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeScreen(
            uiState = uiState,
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { selectedAction = HomeQuickAction.RecentlyPlayed }
        )
    }
}

@Preview(name = "Home loading - Persian", locale = "fa", showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    FuzicTheme {
        val sampleItem = sampleHomeItems().first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeScreen(
            uiState = HomeUiState(isLoading = true),
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { selectedAction = HomeQuickAction.RecentlyPlayed }
        )
    }
}

@Preview(name = "Home empty - Persian", locale = "fa", showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    FuzicTheme {
        val sampleItem = sampleHomeItems().first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var selectedAction by remember { mutableStateOf(HomeQuickAction.LikedSongs) }
        HomeScreen(
            uiState = HomeUiState(),
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { selectedAction = it },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { selectedAction = HomeQuickAction.RecentlyPlayed }
        )
    }
}

@Preview(name = "Home error - Persian", locale = "fa", showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    FuzicTheme {
        val sampleItem = sampleHomeItems().first()
        var selectedItem by remember { mutableStateOf(sampleItem) }
        var retryRequested by remember { mutableStateOf(false) }
        HomeScreen(
            uiState = HomeUiState(errorMessage = stringResource(R.string.home_error_message)),
            onDailyPickClick = { selectedItem = it },
            onQuickActionClick = { },
            onMusicItemClick = { selectedItem = it },
            onRetryClick = { retryRequested = true }
        )
    }
}

@Composable
private fun sampleHomeItems() = listOf(
    FeaturedMusicItem(
        id = "song-1",
        title = stringResource(R.string.preview_song_midnight_drive),
        subtitle = stringResource(R.string.preview_artist_luna_ray)
    ),
    FeaturedMusicItem(
        id = "song-2",
        title = stringResource(R.string.preview_song_tehran_nights),
        subtitle = stringResource(R.string.preview_artist_raha_band)
    ),
    FeaturedMusicItem(
        id = "song-3",
        title = stringResource(R.string.preview_song_golden_echoes),
        subtitle = stringResource(R.string.preview_artist_arman)
    ),
    FeaturedMusicItem(
        id = "song-4",
        title = stringResource(R.string.preview_song_electric_heart),
        subtitle = stringResource(R.string.preview_artist_nika)
    )
)

@Composable
private fun sampleHomeUiState(): HomeUiState {
    val items = sampleHomeItems()
    return HomeUiState(
        dailyPicks = listOf(
            FeaturedMusicItem(
                id = "daily-1",
                title = stringResource(R.string.preview_daily_midnight_vinyl),
                subtitle = stringResource(R.string.preview_daily_midnight_vinyl_subtitle)
            ),
            FeaturedMusicItem(
                id = "daily-2",
                title = stringResource(R.string.preview_daily_local_pulse),
                subtitle = stringResource(R.string.preview_daily_local_pulse_subtitle)
            )
        ),
        sections = listOf(
            HomeMusicSection(R.string.home_section_most_popular, items),
            HomeMusicSection(R.string.home_section_new_releases, items.reversed()),
            HomeMusicSection(R.string.home_section_global_playlists, items),
            HomeMusicSection(R.string.home_section_local_playlists, items.reversed())
        )
    )
}
