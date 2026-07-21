package com.androidprj.fuzic.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.androidprj.fuzic.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.androidprj.fuzic.ui.screens.playlists.AddToPlaylistScreen
import com.androidprj.fuzic.ui.screens.playlists.AddToPlaylistViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileScreen
import com.androidprj.fuzic.ui.screens.profile.ProfileViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileEditorScreen
import com.androidprj.fuzic.ui.screens.profile.ProfileEditorViewModel
import com.androidprj.fuzic.ui.screens.profile.ProfileEditorIntent
import com.androidprj.fuzic.ui.screens.profile.UserProfileScreen
import com.androidprj.fuzic.ui.screens.profile.UserProfileViewModel
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
import com.androidprj.fuzic.ui.screens.liked.LikedSongsScreen
import com.androidprj.fuzic.ui.screens.recentlyplayed.RecentlyPlayedScreen
import com.androidprj.fuzic.ui.screens.songcollection.LikedSongsViewModel
import com.androidprj.fuzic.ui.screens.songcollection.RecentlyPlayedViewModel
import com.androidprj.fuzic.ui.screens.songcollection.SongCollectionIntent
import com.androidprj.fuzic.ui.screens.artists.ArtistsScreen
import com.androidprj.fuzic.ui.screens.artists.ArtistsIntent
import com.androidprj.fuzic.ui.screens.artists.ArtistsViewModel
import com.androidprj.fuzic.ui.screens.notifications.NotificationsScreen
import com.androidprj.fuzic.ui.screens.notifications.NotificationsIntent
import com.androidprj.fuzic.ui.screens.notifications.NotificationsViewModel
import com.androidprj.fuzic.ui.screens.premium.PremiumScreen
import com.androidprj.fuzic.ui.screens.premium.PremiumIntent
import com.androidprj.fuzic.ui.screens.premium.PremiumViewModel
import com.androidprj.fuzic.ui.screens.chat.ChatListScreen
import com.androidprj.fuzic.ui.screens.chat.ChatDetailScreen
import com.androidprj.fuzic.ui.screens.chat.ChatListViewModel
import com.androidprj.fuzic.ui.screens.chat.ChatDetailViewModel
import com.androidprj.fuzic.ui.screens.chat.ChatListIntent
import com.androidprj.fuzic.ui.screens.chat.ChatDetailIntent
import com.androidprj.fuzic.ui.screens.chat.ChatPickerScreen
import com.androidprj.fuzic.ui.screens.chat.ChatPickerViewModel
import com.androidprj.fuzic.ui.screens.follow.FollowSearchScreen
import com.androidprj.fuzic.ui.screens.follow.FollowListScreen
import com.androidprj.fuzic.ui.screens.follow.FollowSearchViewModel
import com.androidprj.fuzic.ui.screens.follow.FollowListViewModel
import com.androidprj.fuzic.ui.screens.follow.FollowSearchIntent
import com.androidprj.fuzic.ui.screens.follow.FollowListIntent
import com.androidprj.fuzic.model.ui.ChatConversation
import com.androidprj.fuzic.model.ui.FollowListType
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.ui.HomeQuickAction
import com.androidprj.fuzic.model.ui.ProfileEntry
import com.androidprj.fuzic.ui.screens.player.PlayerIntent
import com.androidprj.fuzic.ui.screens.player.PlayerScreen
import com.androidprj.fuzic.ui.screens.player.PlayerViewModel
import com.androidprj.fuzic.ui.screens.auth.AuthScreen
import com.androidprj.fuzic.ui.screens.auth.AuthViewModel
import com.androidprj.fuzic.ui.screens.auth.AuthIntent
import com.androidprj.fuzic.ui.screens.auth.WelcomeScreen
import com.androidprj.fuzic.ui.screens.auth.PasswordRecoveryScreen
import com.androidprj.fuzic.model.ui.AuthUiState
import com.androidprj.fuzic.model.ui.WelcomeUiState
import com.androidprj.fuzic.ui.screens.search.SearchIntent
import com.androidprj.fuzic.ui.screens.search.SearchScreen
import com.androidprj.fuzic.ui.screens.search.SearchViewModel
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

@Serializable
data object HomeDestination

@Serializable
data object WelcomeDestination

@Serializable
data object AuthDestination

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

@Serializable
data object LikedSongsDestination

@Serializable
data object RecentlyPlayedDestination

@Serializable
data object ArtistsDestination

@Serializable
data object NotificationsDestination

@Serializable
data object PremiumDestination

@Serializable
data object ChatListDestination

@Serializable
data class ChatDetailDestination(
    val conversationId: String,
    val participantId: String,
    val participantUsername: String,
    val participantDisplayName: String,
    val participantAvatarUrl: String? = null,
)

@Serializable
data object FollowSearchDestination

@Serializable
data class FollowListDestination(val userId: String, val type: String)

@Serializable
data class UserProfileDestination(val userId: String)

@Serializable
data object EditProfileDestination

@Serializable
data object PasswordRecoveryDestination

@Serializable
data class AddToPlaylistDestination(val songId: String)

@Serializable
data class ChatPickerDestination(val songId: String)

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailableMessage = stringResource(R.string.ui_action_unavailable)
    val notificationTargetUnavailableMessage = stringResource(R.string.notification_target_unavailable)
    val unavailableAction: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val currentUser by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val selectedTab = topLevelDestinations.indexOfFirst { destination ->
        currentDestination?.hasRoute(destination::class) == true
    }.takeIf { it >= 0 }?.let(MainTab.entries::get) ?: MainTab.Home
    val showShell = currentDestination?.hasRoute(HomeDestination::class) == true ||
        currentDestination?.hasRoute(SearchDestination::class) == true ||
        currentDestination?.hasRoute(DownloadsDestination::class) == true ||
        currentDestination?.hasRoute(PlaylistsDestination::class) == true ||
        currentDestination?.hasRoute(ProfileDestination::class) == true

    LaunchedEffect(currentUser, showShell) {
        if (currentUser == null && showShell) {
            navController.navigate(WelcomeDestination) {
                popUpTo(HomeDestination) { inclusive = true }
            }
        }
    }

    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        navigationSuiteItems = {
            if (showShell) {
                MainTab.entries.forEachIndexed { index, tab ->
                    item(
                        selected = index == selectedTab.ordinal,
                        onClick = {
                            navController.navigate(topLevelDestinations[index]) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) {
    Scaffold(
        topBar = {
            if (showShell) {
                FuzicTopAppBar(
                    onProfileClick = { navController.navigate(ProfileDestination) },
                    onNotificationsClick = { navController.navigate(NotificationsDestination) },
                    onSettingsClick = { navController.navigate(SettingsDestination) },
                )
            }
        },
        bottomBar = {
            if (showShell) {
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = WelcomeDestination,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable<WelcomeDestination> {
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        navController.navigate(HomeDestination) {
                            popUpTo(WelcomeDestination) { inclusive = true }
                        }
                    }
                }
                WelcomeScreen(
                    uiState = WelcomeUiState(),
                    onPageChanged = { },
                    onSkipClick = { navController.navigate(AuthDestination) },
                    onNextClick = { },
                    onStartClick = { navController.navigate(AuthDestination) },
                )
            }
            composable<AuthDestination> {
                val viewModel: AuthViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        navController.navigate(HomeDestination) {
                            popUpTo(WelcomeDestination) { inclusive = true }
                            popUpTo(AuthDestination) { inclusive = true }
                        }
                    }
                }
                AuthScreen(
                    uiState = uiState,
                    onNameChange = { viewModel.onIntent(AuthIntent.NameChanged(it)) },
                    onEmailChange = { viewModel.onIntent(AuthIntent.EmailChanged(it)) },
                    onPasswordChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
                    onConfirmPasswordChange = { viewModel.onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                    onPasswordVisibilityClick = { viewModel.onIntent(AuthIntent.TogglePasswordVisibility) },
                    onConfirmPasswordVisibilityClick = { viewModel.onIntent(AuthIntent.ToggleConfirmPasswordVisibility) },
                    onSubmitClick = { viewModel.onIntent(AuthIntent.Submit) },
                    onForgotPasswordClick = { navController.navigate(PasswordRecoveryDestination) },
                    onSwitchModeClick = { viewModel.onIntent(AuthIntent.ToggleMode) },
                    onRetryClick = { viewModel.onIntent(AuthIntent.Retry) },
                )
            }
            composable<HomeDestination> {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onDailyPickClick = { item -> navigateForItem(navController, item.id, item.type.name) },
                    onQuickActionClick = { action ->
                        when (action) {
                            HomeQuickAction.LikedSongs -> navController.navigate(LikedSongsDestination)
                            HomeQuickAction.RecentlyPlayed -> navController.navigate(RecentlyPlayedDestination)
                            HomeQuickAction.MyPlaylists -> navController.navigate(PlaylistsDestination)
                            HomeQuickAction.TopArtists -> navController.navigate(ArtistsDestination)
                        }
                    },
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
                    onResultClick = { item ->
                        viewModel.onIntent(SearchIntent.ResultSelected(item))
                        when (item.type) {
                            com.androidprj.fuzic.model.ui.SearchFilter.Songs -> navController.navigate(SongDestination(item.id))
                            com.androidprj.fuzic.model.ui.SearchFilter.Artists -> navController.navigate(ArtistDestination(item.id))
                            com.androidprj.fuzic.model.ui.SearchFilter.Playlists -> navController.navigate(PlaylistDestination(item.id))
                            com.androidprj.fuzic.model.ui.SearchFilter.Users -> navController.navigate(UserProfileDestination(item.id))
                        }
                    },
                    onRetryClick = { viewModel.onIntent(SearchIntent.Retry) },
                )
            }
            composable<DownloadsDestination> {
                val viewModel: DownloadsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DownloadsScreen(
                    uiState = uiState,
                    onSortClick = { viewModel.onIntent(DownloadsIntent.SortSelected(it)) },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
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
                    onEditProfileClick = { navController.navigate(EditProfileDestination) },
                    onEntryClick = { entry ->
                        when (entry) {
                            ProfileEntry.Followers -> uiState.profile?.id?.let { id ->
                                navController.navigate(FollowListDestination(id, FollowListType.Followers.name))
                            }
                            ProfileEntry.Following -> uiState.profile?.id?.let { id ->
                                navController.navigate(FollowListDestination(id, FollowListType.Following.name))
                            }
                            ProfileEntry.LikedSongs -> navController.navigate(LikedSongsDestination)
                            ProfileEntry.RecentlyPlayed -> navController.navigate(RecentlyPlayedDestination)
                            ProfileEntry.Settings -> navController.navigate(SettingsDestination)
                            ProfileEntry.Chat -> navController.navigate(ChatListDestination)
                            ProfileEntry.Logout -> navController.navigate(SettingsDestination)
                        }
                    },
                    onRetryClick = viewModel::retry,
                )
            }
            composable<PasswordRecoveryDestination> {
                var email by rememberSaveable { mutableStateOf("") }
                var isSubmitted by rememberSaveable { mutableStateOf(false) }
                PasswordRecoveryScreen(
                    email = email,
                    isSubmitted = isSubmitted,
                    onBackClick = { navController.popBackStack() },
                    onEmailChange = {
                        email = it
                        isSubmitted = false
                    },
                    onSubmitClick = { isSubmitted = true },
                )
            }
            composable<EditProfileDestination> {
                val viewModel: ProfileEditorViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProfileEditorScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onDisplayNameChange = { viewModel.onIntent(ProfileEditorIntent.DisplayNameChanged(it)) },
                    onUsernameChange = { viewModel.onIntent(ProfileEditorIntent.UsernameChanged(it)) },
                    onAvatarUrlChange = { viewModel.onIntent(ProfileEditorIntent.AvatarUrlChanged(it)) },
                    onSaveClick = { viewModel.onIntent(ProfileEditorIntent.Save) },
                    onRetryClick = { viewModel.onIntent(ProfileEditorIntent.Retry) },
                )
            }
            composable<UserProfileDestination> { entry ->
                val args = entry.toRoute<UserProfileDestination>()
                val viewModel: UserProfileViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(args.userId) { viewModel.load(args.userId) }
                UserProfileScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
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
                    onPlayClick = { playerViewModel.onIntent(PlayerIntent.Play(it)) },
                    onLikeClick = { viewModel.toggleLike() },
                    onDownloadClick = { unavailableAction(unavailableMessage) },
                    onShareClick = { navController.navigate(ChatPickerDestination(it.id)) },
                    onAddToPlaylistClick = { navController.navigate(AddToPlaylistDestination(it.id)) },
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
                    onSongMoreClick = { unavailableAction(unavailableMessage) },
                    onRetryClick = { viewModel.onIntent(PlaylistDetailsIntent.Retry) },
                )
            }
            composable<AddToPlaylistDestination> { entry ->
                val args = entry.toRoute<AddToPlaylistDestination>()
                val viewModel: AddToPlaylistViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val addToPlaylistSuccess = stringResource(R.string.add_to_playlist_success)
                LaunchedEffect(Unit) { viewModel.load() }
                LaunchedEffect(uiState.isComplete) {
                    if (uiState.isComplete) {
                        snackbarHostState.showSnackbar(addToPlaylistSuccess)
                        navController.popBackStack()
                    }
                }
                AddToPlaylistScreen(
                    playlists = uiState.playlists,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onBackClick = { navController.popBackStack() },
                    onPlaylistClick = { viewModel.addSong(it, args.songId) },
                )
            }
            composable<ChatPickerDestination> { entry ->
                val args = entry.toRoute<ChatPickerDestination>()
                val viewModel: ChatPickerViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val shareSuccess = stringResource(R.string.share_to_chat_success)
                LaunchedEffect(uiState.isComplete) {
                    if (uiState.isComplete) {
                        snackbarHostState.showSnackbar(shareSuccess)
                        navController.popBackStack()
                    }
                }
                ChatPickerScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onConversationClick = { viewModel.share(it, args.songId) },
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
                    onSongMoreClick = { unavailableAction(unavailableMessage) },
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
                    onShareClick = { playerUiState.currentSong?.let { navController.navigate(ChatPickerDestination(it.id)) } ?: unavailableAction(unavailableMessage) },
                    onAddToPlaylistClick = { playerUiState.currentSong?.let { navController.navigate(AddToPlaylistDestination(it.id)) } ?: unavailableAction(unavailableMessage) },
                    onQueueClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.Queue)) },
                    onSleepTimerClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.SleepTimer)) },
                    onPlaybackSpeedClick = { playerViewModel.onIntent(PlayerIntent.ShowOverlay(com.androidprj.fuzic.model.ui.PlayerOverlay.PlaybackSpeed)) },
                    onQueueSongClick = { playerViewModel.onIntent(PlayerIntent.QueueSongSelected(it)) },
                    onSongMoreClick = { unavailableAction(unavailableMessage) },
                    onOverlayDismiss = { playerViewModel.onIntent(PlayerIntent.DismissOverlay) },
                    onSleepTimerSelected = { playerViewModel.onIntent(PlayerIntent.SleepTimerSelected(it)) },
                    onPlaybackSpeedSelected = { playerViewModel.onIntent(PlayerIntent.PlaybackSpeedSelected(it)) },
                )
            }
            composable<LikedSongsDestination> {
                val viewModel: LikedSongsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LikedSongsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onSongMoreClick = { unavailableAction(unavailableMessage) },
                    onRetryClick = { viewModel.onIntent(SongCollectionIntent.Retry) },
                )
            }
            composable<RecentlyPlayedDestination> {
                val viewModel: RecentlyPlayedViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RecentlyPlayedScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onSongMoreClick = { unavailableAction(unavailableMessage) },
                    onRetryClick = { viewModel.onIntent(SongCollectionIntent.Retry) },
                )
            }
            composable<ArtistsDestination> {
                val viewModel: ArtistsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ArtistsScreen(
                    uiState = uiState,
                    onArtistClick = { navController.navigate(ArtistDestination(it.id)) },
                    onFollowClick = { viewModel.onIntent(ArtistsIntent.ToggleFollow(it)) },
                    onRetryClick = { viewModel.onIntent(ArtistsIntent.Retry) },
                )
            }
            composable<NotificationsDestination> {
                val viewModel: NotificationsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                NotificationsScreen(
                    uiState = uiState,
                    onNotificationClick = {
                        viewModel.onIntent(NotificationsIntent.NotificationSelected(it))
                        unavailableAction(notificationTargetUnavailableMessage)
                    },
                    onMarkAllReadClick = { viewModel.onIntent(NotificationsIntent.MarkAllRead) },
                    onRetryClick = { viewModel.onIntent(NotificationsIntent.Retry) },
                )
            }
            composable<PremiumDestination> {
                val viewModel: PremiumViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                PremiumScreen(
                    uiState = uiState,
                    onPlanSelected = { viewModel.onIntent(PremiumIntent.SelectPlan(it)) },
                    onUpgradeClick = { viewModel.onIntent(PremiumIntent.Upgrade) },
                    onRestoreClick = { viewModel.onIntent(PremiumIntent.RestorePurchase) },
                    onRetryClick = { viewModel.onIntent(PremiumIntent.Retry) },
                )
            }
            composable<ChatListDestination> {
                val viewModel: ChatListViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ChatListScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onConversationClick = { conversation ->
                        navController.navigate(
                            ChatDetailDestination(
                                conversationId = conversation.id,
                                participantId = conversation.participant.id,
                                participantUsername = conversation.participant.username,
                                participantDisplayName = conversation.participant.displayName,
                                participantAvatarUrl = conversation.participant.avatarUrl,
                            ),
                        )
                    },
                    onRetryClick = { viewModel.onIntent(ChatListIntent.Retry) },
                )
            }
            composable<ChatDetailDestination> { entry ->
                val args = entry.toRoute<ChatDetailDestination>()
                val viewModel: ChatDetailViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val conversation = ChatConversation(
                    id = args.conversationId,
                    participant = FollowUser(
                        id = args.participantId,
                        username = args.participantUsername,
                        displayName = args.participantDisplayName,
                        avatarUrl = args.participantAvatarUrl,
                    ),
                    lastMessagePreview = "",
                    lastMessageTimeLabel = "",
                )
                LaunchedEffect(conversation.id) {
                    viewModel.onIntent(ChatDetailIntent.LoadConversation(conversation))
                }
                ChatDetailScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onDraftChange = { viewModel.onIntent(ChatDetailIntent.DraftChanged(it)) },
                    onSendClick = { viewModel.onIntent(ChatDetailIntent.SendDraft) },
                    onShareSongClick = { unavailableAction(unavailableMessage) },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onRetryClick = { viewModel.onIntent(ChatDetailIntent.Retry) },
                )
            }
            composable<FollowSearchDestination> {
                val viewModel: FollowSearchViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                FollowSearchScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onQueryChange = { viewModel.onIntent(FollowSearchIntent.QueryChanged(it)) },
                    onUserClick = { navController.navigate(UserProfileDestination(it.id)) },
                    onFollowClick = { viewModel.onIntent(FollowSearchIntent.ToggleFollow(it)) },
                    onRetryClick = { viewModel.onIntent(FollowSearchIntent.Retry) },
                )
            }
            composable<FollowListDestination> { entry ->
                val args = entry.toRoute<FollowListDestination>()
                val viewModel: FollowListViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val type = runCatching { FollowListType.valueOf(args.type) }.getOrDefault(FollowListType.Followers)
                LaunchedEffect(args.userId, type) {
                    viewModel.onIntent(FollowListIntent.Load(args.userId, type))
                }
                FollowListScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { navController.navigate(UserProfileDestination(it.id)) },
                    onFollowClick = { viewModel.onIntent(FollowListIntent.ToggleFollow(it)) },
                    onRetryClick = { viewModel.onIntent(FollowListIntent.Retry) },
                )
            }
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
