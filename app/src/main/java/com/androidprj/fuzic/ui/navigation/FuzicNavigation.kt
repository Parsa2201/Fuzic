package com.androidprj.fuzic.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.androidprj.fuzic.ui.components.FuzicTopAppBar
import com.androidprj.fuzic.ui.components.MiniPlayer
import com.androidprj.fuzic.model.ui.MiniPlayerUiState
import com.androidprj.fuzic.ui.screens.downloads.DownloadsIntent
import com.androidprj.fuzic.ui.screens.downloads.DownloadsScreen
import com.androidprj.fuzic.ui.screens.downloads.DownloadsViewModel
import com.androidprj.fuzic.ui.screens.home.HomeScreen
import com.androidprj.fuzic.ui.screens.home.HomeViewModel
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsIntent
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsScreen
import com.androidprj.fuzic.ui.screens.playlists.PlaylistsViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileScreen
import com.androidprj.fuzic.ui.screens.profile.ProfileViewModel
import com.androidprj.fuzic.ui.screens.song.SongDetailsScreen
import com.androidprj.fuzic.ui.screens.song.SongDetailsViewModel
import com.androidprj.fuzic.ui.screens.playlistdetail.PlaylistDetailsScreen
import com.androidprj.fuzic.ui.screens.playlistdetail.PlaylistDetailsIntent
import com.androidprj.fuzic.ui.screens.playlistdetail.PlaylistDetailsViewModel
import com.androidprj.fuzic.ui.screens.artist.ArtistDetailsScreen
import com.androidprj.fuzic.ui.screens.artist.ArtistDetailsIntent
import com.androidprj.fuzic.ui.screens.artist.ArtistDetailsViewModel
import com.androidprj.fuzic.ui.screens.settings.SettingsIntent
import com.androidprj.fuzic.ui.screens.settings.SettingsScreen
import com.androidprj.fuzic.ui.screens.settings.SettingsViewModel
import com.androidprj.fuzic.ui.screens.player.PlayerIntent
import com.androidprj.fuzic.ui.screens.player.PlayerScreen
import com.androidprj.fuzic.ui.screens.player.PlayerViewModel
import com.androidprj.fuzic.ui.screens.search.SearchIntent
import com.androidprj.fuzic.ui.screens.search.SearchScreen
import com.androidprj.fuzic.ui.screens.search.SearchViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data object SearchDestination

@Serializable
data object DownloadsDestination

@Serializable
data object PlaylistsDestination

@Serializable
data object ProfileDestination

@Serializable
data class SongDestination(val songId: String)

@Serializable
data class PlaylistDestination(val playlistId: String)

@Serializable
data class ArtistDestination(val artistId: String)

@Serializable
data object SettingsDestination

@Serializable
data object FullPlayerDestination

private val topLevelDestinations = listOf(
    HomeDestination,
    SearchDestination,
    DownloadsDestination,
    PlaylistsDestination,
    ProfileDestination,
)

@Composable
fun FuzicNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val selectedTab = topLevelDestinations.indexOfFirst { destination ->
        currentDestination?.hasRoute(destination::class) == true
    }.takeIf { it >= 0 }?.let(MainTab.entries::get) ?: MainTab.Home

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FuzicTopAppBar(
                onProfileClick = { navController.navigate(ProfileDestination) },
                onNotificationsClick = { },
                onSettingsClick = { navController.navigate(SettingsDestination) },
            )
        },
        bottomBar = {
            Column {
                playerUiState.currentSong?.let { song ->
                    MiniPlayer(
                        uiState = MiniPlayerUiState(
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.artworkUrl,
                            isPlaying = playerUiState.isPlaying,
                        ),
                        onClick = { navController.navigate(FullPlayerDestination) },
                        onPlayPauseClick = { playerViewModel.onIntent(PlayerIntent.TogglePlayPause) },
                    )
                }
                NavigationBar {
                    MainTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = index == selectedTab.ordinal,
                            onClick = {
                                navController.navigate(topLevelDestinations[index]) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(HomeDestination) { saveState = true }
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable<HomeDestination> {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onDailyPickClick = { item -> navigateForItem(navController, item.id, item.type.name) },
                    onQuickActionClick = { },
                    onMusicItemClick = { item -> navigateForItem(navController, item.id, item.type.name) },
                    onRetryClick = viewModel::retry,
                )
            }
            composable<SearchDestination> {
                val viewModel: SearchViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SearchScreen(
                    uiState = uiState,
                    onQueryChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
                    onFilterClick = { viewModel.onIntent(SearchIntent.FilterSelected(it)) },
                    onHistoryClick = { viewModel.onIntent(SearchIntent.HistorySelected(it)) },
                    onHistoryDeleteClick = { viewModel.onIntent(SearchIntent.DeleteHistory(it)) },
                    onClearHistoryClick = { viewModel.onIntent(SearchIntent.ClearHistory) },
                    onResultClick = { viewModel.onIntent(SearchIntent.ResultSelected(it)) },
                    onRetryClick = { viewModel.onIntent(SearchIntent.Retry) },
                )
            }
            composable<DownloadsDestination> {
                val viewModel: DownloadsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DownloadsScreen(
                    uiState = uiState,
                    onSortClick = { viewModel.onIntent(DownloadsIntent.SortSelected(it)) },
                    onSongClick = { },
                    onDeleteClick = { viewModel.onIntent(DownloadsIntent.Delete(it)) },
                    onUndoDeleteClick = { viewModel.onIntent(DownloadsIntent.UndoDelete) },
                    onRetryClick = { viewModel.onIntent(DownloadsIntent.Retry) },
                    onFreeUpSpaceClick = { viewModel.onIntent(DownloadsIntent.FreeUpSpace) },
                )
            }
            composable<PlaylistsDestination> {
                val viewModel: PlaylistsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                PlaylistsScreen(
                    uiState = uiState,
                    onPlaylistClick = { navController.navigate(PlaylistDestination(it.id)) },
                    onNewPlaylistClick = { viewModel.onIntent(PlaylistsIntent.ShowCreate) },
                    onCreateNameChange = { viewModel.onIntent(PlaylistsIntent.NameChanged(it)) },
                    onCreateCoverSelected = { viewModel.onIntent(PlaylistsIntent.CoverChanged(it)) },
                    onCreateConfirmClick = { viewModel.onIntent(PlaylistsIntent.Create) },
                    onCreateDismissClick = { viewModel.onIntent(PlaylistsIntent.DismissCreate) },
                    onRetryClick = { viewModel.onIntent(PlaylistsIntent.Retry) },
                )
            }
            composable<ProfileDestination> {
                val viewModel: ProfileViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProfileScreen(
                    uiState = uiState,
                    onEditProfileClick = { },
                    onEntryClick = { },
                    onRetryClick = viewModel::retry,
                )
            }
            composable<SongDestination> { entry ->
                val args = entry.toRoute<SongDestination>()
                val viewModel: SongDetailsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(args.songId) { viewModel.load(args.songId) }
                SongDetailsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { },
                    onLikeClick = { viewModel.toggleLike() },
                    onDownloadClick = { },
                    onShareClick = { },
                    onAddToPlaylistClick = { },
                    onRetryClick = { viewModel.load(args.songId) },
                )
            }
            composable<PlaylistDestination> { entry ->
                val args = entry.toRoute<PlaylistDestination>()
                val viewModel: PlaylistDetailsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(args.playlistId) {
                    viewModel.onIntent(PlaylistDetailsIntent.Load(args.playlistId))
                }
                PlaylistDetailsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onPlayAllClick = { viewModel.onIntent(PlaylistDetailsIntent.PlayAll(it)) },
                    onSongClick = { song -> navController.navigate(SongDestination(song.id)) },
                    onSongMoreClick = { },
                    onRetryClick = { viewModel.onIntent(PlaylistDetailsIntent.Retry) },
                )
            }
            composable<ArtistDestination> { entry ->
                val args = entry.toRoute<ArtistDestination>()
                val viewModel: ArtistDetailsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(args.artistId) {
                    viewModel.onIntent(ArtistDetailsIntent.Load(args.artistId))
                }
                ArtistDetailsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onFollowClick = { viewModel.onIntent(ArtistDetailsIntent.ToggleFollow) },
                    onPlaySongClick = { viewModel.onIntent(ArtistDetailsIntent.PlaySong(it)) },
                    onSongMoreClick = { },
                    onRetryClick = { viewModel.onIntent(ArtistDetailsIntent.Retry) },
                )
            }
            composable<SettingsDestination> {
                val viewModel: SettingsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onThemeClick = { viewModel.onIntent(SettingsIntent.ShowThemeOptions) },
                    onLanguageClick = { viewModel.onIntent(SettingsIntent.ShowLanguageOptions) },
                    onLogoutClick = { viewModel.onIntent(SettingsIntent.ShowLogoutConfirmation) },
                    onLogoutConfirm = { viewModel.onIntent(SettingsIntent.ConfirmLogout) },
                    onLogoutDismiss = { viewModel.onIntent(SettingsIntent.DismissLogoutConfirmation) },
                    onThemeSelected = { viewModel.onIntent(SettingsIntent.ThemeSelected(it)) },
                    onLanguageSelected = { viewModel.onIntent(SettingsIntent.LanguageSelected(it)) },
                    onRetryClick = { viewModel.onIntent(SettingsIntent.Retry) },
                )
            }
            composable<FullPlayerDestination> {
                PlayerScreen(
                    uiState = playerUiState,
                    onCloseClick = { navController.popBackStack() },
                    onPreviousClick = { playerViewModel.onIntent(PlayerIntent.Previous) },
                    onPlayPauseClick = { playerViewModel.onIntent(PlayerIntent.TogglePlayPause) },
                    onNextClick = { playerViewModel.onIntent(PlayerIntent.Next) },
                    onSeek = { playerViewModel.onIntent(PlayerIntent.Seek(it)) },
                    onShuffleClick = { playerViewModel.onIntent(PlayerIntent.ToggleShuffle) },
                    onRepeatClick = { playerViewModel.onIntent(PlayerIntent.CycleRepeatMode) },
                    onLikeClick = { playerViewModel.onIntent(PlayerIntent.ToggleLike) },
                    onShareClick = { },
                    onAddToPlaylistClick = { },
                    onQueueClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.Queue)) },
                    onSleepTimerClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.SleepTimer)) },
                    onPlaybackSpeedClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.PlaybackSpeed)) },
                    onQueueSongClick = { playerViewModel.onIntent(PlayerIntent.QueueSongSelected(it)) },
                    onSongMoreClick = { },
                    onOverlayDismiss = { playerViewModel.onIntent(PlayerIntent.DismissOverlay) },
                    onSleepTimerSelected = { playerViewModel.onIntent(PlayerIntent.SleepTimerSelected(it)) },
                    onPlaybackSpeedSelected = { playerViewModel.onIntent(PlayerIntent.PlaybackSpeedSelected(it)) },
                )
            }
        }
    }
}

private fun navigateForItem(controller: NavHostController, id: String, type: String) {
    when (type) {
        "Playlist" -> controller.navigate(PlaylistDestination(id))
        "Artist" -> controller.navigate(ArtistDestination(id))
        else -> controller.navigate(SongDestination(id))
    }
}
