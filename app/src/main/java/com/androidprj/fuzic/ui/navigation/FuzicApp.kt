package com.androidprj.fuzic.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.androidprj.fuzic.R
import com.androidprj.fuzic.model.ui.CreatePlaylistUiState
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.model.ui.DownloadsUiState
import com.androidprj.fuzic.model.ui.FeaturedMusicItem
import com.androidprj.fuzic.model.ui.HomeMusicSection
import com.androidprj.fuzic.model.ui.HomeQuickAction
import com.androidprj.fuzic.model.ui.HomeUiState
import com.androidprj.fuzic.model.ui.MiniPlayerUiState
import com.androidprj.fuzic.model.ui.MusicItemType
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.PlaylistSection
import com.androidprj.fuzic.model.ui.PlaylistSectionType
import com.androidprj.fuzic.model.ui.PlaylistsUiState
import com.androidprj.fuzic.model.ui.ProfileEntry
import com.androidprj.fuzic.model.ui.ProfileStats
import com.androidprj.fuzic.model.ui.ProfileUiState
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.model.ui.SearchUiState
import com.androidprj.fuzic.ui.components.FuzicTopAppBar
import com.androidprj.fuzic.ui.components.MiniPlayer
import com.androidprj.fuzic.ui.components.previewArtworkUri
import com.androidprj.fuzic.ui.screens.downloads.DownloadsScreen
import com.androidprj.fuzic.ui.screens.home.HomeScreen
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsScreen
import com.androidprj.fuzic.ui.screens.profile.ProfileScreen
import com.androidprj.fuzic.ui.screens.chat.ChatListScreen
import com.androidprj.fuzic.model.ui.ChatListUiState
import com.androidprj.fuzic.ui.screens.search.SearchScreen
import com.androidprj.fuzic.ui.theme.FuzicTheme
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppThemeOption
import java.util.Locale

@Composable
fun FuzicApp(modifier: Modifier = Modifier) {
    val viewModel: AppSettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val darkTheme = when (settings.theme) {
        AppThemeOption.System -> androidx.compose.foundation.isSystemInDarkTheme()
        AppThemeOption.Light -> false
        AppThemeOption.Dark -> true
    }
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
    val locale = when (settings.language) {
        AppLanguageOption.System -> null
        AppLanguageOption.English -> Locale.ENGLISH
        AppLanguageOption.Persian -> Locale.forLanguageTag("fa")
    }
    val localizedConfiguration = android.content.res.Configuration(baseConfiguration).apply {
        locale?.let(::setLocale)
        setLayoutDirection(locale ?: Locale.getDefault())
    }
    val localizedResources = baseContext.createConfigurationContext(localizedConfiguration).resources
    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalResources provides localizedResources,
        androidx.compose.ui.platform.LocalLayoutDirection provides if (locale?.language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
        androidx.compose.ui.platform.LocalDensity provides Density(baseDensity.density, settings.fontScale.multiplier),
    ) {
        FuzicTheme(darkTheme = darkTheme) {
            FuzicNavigation(modifier = modifier)
        }
    }
}

@Composable
fun FuzicAppShell(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniPlayerState: MiniPlayerUiState? = sampleMiniPlayerUiState()
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            MainTab.entries.forEach { tab ->
                item(
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes)
                        )
                    },
                    label = { Text(stringResource(tab.labelRes)) },
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                FuzicTopAppBar(
                    avatarUrl = null,
                    onProfileClick = onProfileClick,
                    onNotificationsClick = {},
                    onSettingsClick = onSettingsClick
                )
            },
            bottomBar = {
                miniPlayerState?.let { uiState ->
                    MiniPlayer(
                        uiState = uiState,
                        onClick = {},
                        onPlayPauseClick = {},
                        onCloseClick = {}
                    )
                }
            }
        ) { innerPadding ->
            FuzicMainTabContent(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                contentPadding = innerPadding
            )
        }
    }
}

@Composable
private fun FuzicMainTabContent(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val contentModifier = modifier
        .fillMaxSize()
        .padding(contentPadding)

    when (selectedTab) {
        MainTab.Home -> HomeScreen(
            uiState = sampleHomeUiState(),
            onDailyPickClick = {},
            onQuickActionClick = { action ->
                when (action) {
                    HomeQuickAction.MyPlaylists -> onTabSelected(MainTab.Playlists)
                    else -> Unit
                }
            },
            onMusicItemClick = {},
            onRetryClick = {},
            modifier = contentModifier
        )
        MainTab.Search -> SearchScreen(
            uiState = sampleSearchUiState(),
            onQueryChange = {},
            onSearchSubmit = {},
            onFilterClick = {},
            onHistoryClick = {},
            onHistoryDeleteClick = {},
            onClearHistoryClick = {},
            onResultClick = {},
            onRetryClick = {},
            resultsFlow = kotlinx.coroutines.flow.flowOf(sampleSearchUiState().results),
            modifier = contentModifier
        )
        MainTab.Downloads -> DownloadsScreen(
            uiState = sampleDownloadsUiState(),
            onSortClick = {},
            onSongClick = {},
            onDeleteClick = {},
            onUndoDeleteClick = {},
            onRetryClick = {},
            onFreeUpSpaceClick = {},
            onUpgradeClick = {},
            modifier = contentModifier
        )
        MainTab.Playlists -> PlaylistsScreen(
            uiState = samplePlaylistsUiState(),
            onPlaylistClick = {},
            onNewPlaylistClick = {},
            onCreateNameChange = {},
            onCreateCoverSelected = {},
            onCreateConfirmClick = {},
            onCreateDismissClick = {},
            onRetryClick = {},
            modifier = contentModifier
        )
        MainTab.Chat -> ChatListScreen(
            uiState = ChatListUiState(),
            onConversationClick = {},
            onRetryClick = {},
            modifier = contentModifier
        )
    }
}

@Composable
private fun sampleMiniPlayerUiState(): MiniPlayerUiState = MiniPlayerUiState(
    title = stringResource(R.string.preview_song_midnight_drive),
    artist = stringResource(R.string.preview_artist_luna_ray),
    artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight),
    isPlaying = true
)

@Composable
private fun sampleHomeUiState(): HomeUiState {
    val midnightArtwork = previewArtworkUri(R.drawable.preview_artwork_midnight)
    val tehranArtwork = previewArtworkUri(R.drawable.preview_artwork_tehran)
    val echoesArtwork = previewArtworkUri(R.drawable.preview_artwork_echoes)
    val pulseArtwork = previewArtworkUri(R.drawable.preview_artwork_pulse)
    val items = listOf(
        FeaturedMusicItem(
            id = "song-midnight-drive",
            title = stringResource(R.string.preview_song_midnight_drive),
            subtitle = stringResource(R.string.preview_artist_luna_ray),
            artworkUrl = midnightArtwork
        ),
        FeaturedMusicItem(
            id = "song-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            subtitle = stringResource(R.string.preview_artist_raha_band),
            artworkUrl = tehranArtwork
        ),
        FeaturedMusicItem(
            id = "song-golden-echoes",
            title = stringResource(R.string.preview_song_golden_echoes),
            subtitle = stringResource(R.string.preview_artist_arman),
            artworkUrl = echoesArtwork
        ),
        FeaturedMusicItem(
            id = "song-electric-heart",
            title = stringResource(R.string.preview_song_electric_heart),
            subtitle = stringResource(R.string.preview_artist_nika),
            artworkUrl = pulseArtwork
        )
    )

    return HomeUiState(
        dailyPicks = listOf(
            FeaturedMusicItem(
                id = "daily-midnight-vinyl",
                title = stringResource(R.string.preview_daily_midnight_vinyl),
                subtitle = stringResource(R.string.preview_daily_midnight_vinyl_subtitle),
                artworkUrl = midnightArtwork,
                type = MusicItemType.Album
            ),
            FeaturedMusicItem(
                id = "daily-local-pulse",
                title = stringResource(R.string.preview_daily_local_pulse),
                subtitle = stringResource(R.string.preview_daily_local_pulse_subtitle),
                artworkUrl = pulseArtwork,
                type = MusicItemType.Playlist
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

@Composable
private fun sampleSearchUiState(): SearchUiState = SearchUiState(
    query = stringResource(R.string.preview_search_query),
    selectedFilter = SearchFilter.Songs,
    history = listOf(
        stringResource(R.string.preview_search_history_one),
        stringResource(R.string.preview_search_history_two),
        stringResource(R.string.preview_search_history_three)
    ),
    results = androidx.paging.PagingData.from(listOf(
        SearchResultItem(
            id = "result-midnight-drive",
            title = stringResource(R.string.preview_song_midnight_drive),
            subtitle = stringResource(R.string.preview_artist_luna_ray),
            type = SearchFilter.Songs,
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
        ),
        SearchResultItem(
            id = "result-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            subtitle = stringResource(R.string.preview_artist_raha_band),
            type = SearchFilter.Songs,
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
        )
    ))
)

@Composable
private fun sampleDownloadsUiState() = DownloadsUiState(
    downloads = listOf(
        DownloadedSongItem(
            id = "download-1",
            title = stringResource(R.string.preview_song_midnight_drive),
            artist = stringResource(R.string.preview_artist_luna_ray),
            fileSizeLabel = stringResource(R.string.preview_download_size_large),
            downloadedAtLabel = stringResource(R.string.preview_download_date_today),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
        ),
        DownloadedSongItem(
            id = "download-tehran-nights",
            title = stringResource(R.string.preview_song_tehran_nights),
            artist = stringResource(R.string.preview_artist_raha_band),
            fileSizeLabel = stringResource(R.string.preview_download_size_medium),
            downloadedAtLabel = stringResource(R.string.preview_download_date_yesterday),
            artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
        )
    ),
    isPremiumUser = true,
    isPremiumLoading = false
)

@Composable
private fun samplePlaylistsUiState(): PlaylistsUiState = PlaylistsUiState(
    sections = listOf(
        PlaylistSection(
            titleRes = R.string.playlists_section_global,
            type = PlaylistSectionType.Global,
            playlists = samplePlaylistItems()
        ),
        PlaylistSection(
            titleRes = R.string.playlists_section_local,
            type = PlaylistSectionType.Local,
            playlists = samplePlaylistItems().reversed()
        ),
        PlaylistSection(
            titleRes = R.string.playlists_section_mine,
            type = PlaylistSectionType.Mine,
            playlists = samplePlaylistItems()
        )
    ),
    createPlaylistState = CreatePlaylistUiState()
)

@Composable
private fun samplePlaylistItems(): List<PlaylistItem> = listOf(
    PlaylistItem(
        id = "playlist-global-hits",
        title = stringResource(R.string.preview_playlist_global_hits),
        subtitle = stringResource(R.string.preview_artist_luna_ray),
        songCountLabel = stringResource(R.string.preview_playlist_song_count_large),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_midnight)
    ),
    PlaylistItem(
        id = "playlist-evening-mix",
        title = stringResource(R.string.preview_playlist_evening_mix),
        subtitle = stringResource(R.string.preview_artist_raha_band),
        songCountLabel = stringResource(R.string.preview_playlist_song_count_medium),
        artworkUrl = previewArtworkUri(R.drawable.preview_artwork_tehran)
    )
)

@Composable
private fun sampleProfileUiState(): ProfileUiState = ProfileUiState(
    profile = ProfileUser(
        id = "profile-parsa",
        username = stringResource(R.string.preview_profile_username),
        displayName = stringResource(R.string.preview_profile_display_name),
        avatarUrl = previewArtworkUri(R.drawable.preview_artwork_pulse),
        isPremium = true
    ),
    stats = ProfileStats(
        followersLabel = stringResource(R.string.preview_profile_followers_count),
        followingLabel = stringResource(R.string.preview_profile_following_count),
        playlistsLabel = stringResource(R.string.preview_profile_playlists_count)
    )
)

@PreviewScreenSizes
@Composable
private fun FuzicAppPreview() {
    FuzicTheme {
        FuzicApp()
    }
}

@Preview(name = "App shell - search", showBackground = true)
@Composable
private fun FuzicAppShellSearchPreview() {
    FuzicTheme {
        FuzicAppShell(
            selectedTab = MainTab.Search,
            onTabSelected = {},
            onProfileClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(name = "App shell - Persian profile", locale = "fa", showBackground = true)
@Composable
private fun FuzicAppShellPersianPreview() {
    FuzicTheme {
        FuzicAppShell(
            selectedTab = MainTab.Chat,
            onTabSelected = {},
            onProfileClick = {},
            onSettingsClick = {}
        )
    }
}
