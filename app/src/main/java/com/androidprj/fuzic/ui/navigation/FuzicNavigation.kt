package com.androidprj.fuzic.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.androidprj.fuzic.ui.components.FuzicTopAppBar
import com.androidprj.fuzic.ui.components.MiniPlayer
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
            composable<SongDestination> { }
            composable<PlaylistDestination> { }
            composable<ArtistDestination> { }
            composable<SettingsDestination> { }
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
