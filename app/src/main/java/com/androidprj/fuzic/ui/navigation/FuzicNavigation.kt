package com.androidprj.fuzic.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
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
import com.androidprj.fuzic.ui.components.SongActionSheet
import com.androidprj.fuzic.model.ui.MiniPlayerUiState
import com.androidprj.fuzic.model.ui.NotificationTarget
import com.androidprj.fuzic.model.ui.SongItem
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
import com.androidprj.fuzic.model.ui.ChatMessage
import com.androidprj.fuzic.model.ui.FollowListType
import com.androidprj.fuzic.model.ui.FollowUser
import com.androidprj.fuzic.model.ui.HomeQuickAction
import com.androidprj.fuzic.model.ui.MusicItemType
import com.androidprj.fuzic.model.ui.ProfileEntry
import com.androidprj.fuzic.ui.screens.player.PlayerIntent
import com.androidprj.fuzic.ui.screens.player.PlayerScreen
import com.androidprj.fuzic.ui.screens.player.PlayerViewModel
import com.androidprj.fuzic.ui.screens.auth.AuthScreen
import com.androidprj.fuzic.ui.screens.auth.AuthViewModel
import com.androidprj.fuzic.ui.screens.auth.AuthIntent
import com.androidprj.fuzic.ui.screens.auth.WelcomeScreen
import com.androidprj.fuzic.ui.screens.auth.PasswordRecoveryScreen
import com.androidprj.fuzic.ui.screens.auth.PasswordRecoveryViewModel
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
data class SettingsDestination(val showLogoutConfirmation: Boolean = false)

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
data class FollowListDestination(val userId: String, val type: FollowListType)

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

private const val ProfileUpdatedResultKey = "profile_updated"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FuzicNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailableMessage = stringResource(R.string.ui_action_unavailable)
    val notificationTargetUnavailableMessage = stringResource(R.string.notification_target_unavailable)
    val profileSavedMessage = stringResource(R.string.edit_profile_saved)
    val unavailableAction: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionUiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    if (sessionUiState is SessionUiState.Restoring) {
        SessionRestoreScreen(modifier)
        return
    }
    val currentUser = (sessionUiState as SessionUiState.Ready).currentUser
    var shellAvatarUrl by remember(currentUser?.id) { mutableStateOf(currentUser?.avatarUrl) }
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    var songActionTarget by remember { mutableStateOf<SongItem?>(null) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val selectedMainTab = topLevelDestinations.indexOfFirst { destination ->
        currentDestination?.hasRoute(destination::class) == true
    }.takeIf { it >= 0 }?.let(MainTab.entries::get)
    var lastMainTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    LaunchedEffect(selectedMainTab) {
        selectedMainTab?.let { lastMainTab = it }
    }
    val isTabSubDestination = currentDestination?.hasRoute(LikedSongsDestination::class) == true ||
        currentDestination?.hasRoute(RecentlyPlayedDestination::class) == true ||
        currentDestination?.hasRoute(ArtistsDestination::class) == true ||
        currentDestination?.hasRoute(SongDestination::class) == true ||
        currentDestination?.hasRoute(PlaylistDestination::class) == true ||
        currentDestination?.hasRoute(ArtistDestination::class) == true ||
        currentDestination?.hasRoute(ChatListDestination::class) == true ||
        currentDestination?.hasRoute(ChatDetailDestination::class) == true ||
        currentDestination?.hasRoute(FollowSearchDestination::class) == true ||
        currentDestination?.hasRoute(FollowListDestination::class) == true ||
        currentDestination?.hasRoute(UserProfileDestination::class) == true
    // Detail/subpages stay visually attached to the tab from which they were opened.
    val selectedTab = selectedMainTab ?: if (isTabSubDestination) lastMainTab else MainTab.Home
    val isMainTabDestination = currentDestination?.hasRoute(HomeDestination::class) == true ||
        currentDestination?.hasRoute(SearchDestination::class) == true ||
        currentDestination?.hasRoute(DownloadsDestination::class) == true ||
        currentDestination?.hasRoute(PlaylistsDestination::class) == true ||
        currentDestination?.hasRoute(ProfileDestination::class) == true
    val showBottomNavigation = isMainTabDestination || isTabSubDestination
    val hideMiniPlayer = currentDestination?.hasRoute(WelcomeDestination::class) == true ||
        currentDestination?.hasRoute(AuthDestination::class) == true ||
        currentDestination?.hasRoute(PasswordRecoveryDestination::class) == true ||
        currentDestination?.hasRoute(FullPlayerDestination::class) == true ||
        currentDestination?.hasRoute(EditProfileDestination::class) == true ||
        currentDestination?.hasRoute(AddToPlaylistDestination::class) == true ||
        currentDestination?.hasRoute(ChatPickerDestination::class) == true
    val isFullPlayerOpen = currentDestination?.hasRoute(FullPlayerDestination::class) == true

    LaunchedEffect(currentUser, showBottomNavigation) {
        if (currentUser == null && showBottomNavigation) {
            navController.navigate(WelcomeDestination) {
                popUpTo(HomeDestination) { inclusive = true }
            }
        }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = isFullPlayerOpen,
            label = "full_player_transition",
        ) { fullPlayerVisible ->
            NavigationSuiteScaffold(
                modifier = modifier.fillMaxSize(),
                layoutType = if (showBottomNavigation) {
                    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
                } else {
                    NavigationSuiteType.None
                },
                navigationSuiteItems = {
                    if (showBottomNavigation) {
                        MainTab.entries.forEachIndexed { index, tab ->
                            item(
                                selected = index == selectedTab.ordinal,
                                onClick = {
                                    lastMainTab = tab
                                    if (tab == MainTab.Home) {
                                        val returnedToHome = navController.popBackStack(HomeDestination, inclusive = false)
                                        if (!returnedToHome) {
                                            navController.navigate(HomeDestination) {
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(topLevelDestinations[index]) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
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
            val isWelcomeDestination = currentDestination?.hasRoute(WelcomeDestination::class) == true
            Scaffold(
                contentWindowInsets = if (isWelcomeDestination) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
                topBar = {
                    if (isMainTabDestination) {
                        FuzicTopAppBar(
                            avatarUrl = shellAvatarUrl,
                            onProfileClick = { navController.navigate(ProfileDestination) },
                            onNotificationsClick = { navController.navigate(NotificationsDestination) },
                            onSettingsClick = { navController.navigate(SettingsDestination()) },
                        )
                    }
                },
                bottomBar = {
                    if (!hideMiniPlayer || !fullPlayerVisible) {
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
                                // NavigationSuiteScaffold reserves the system navigation area when the
                                // tab bar is visible. Screens outside that shell still need this inset
                                // so playback controls never render behind Android's navigation buttons.
                                modifier = if (showBottomNavigation) {
                                    Modifier
                                } else {
                                    Modifier.navigationBarsPadding()
                                },
                                artworkModifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState("player-artwork-${song.id}"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                ),
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                val navHostModifier = if (isWelcomeDestination) {
                    Modifier
                } else {
                    Modifier.padding(paddingValues)
                }
                NavHost(
                    navController = navController,
                    startDestination = if (currentUser != null) HomeDestination else WelcomeDestination,
                    modifier = navHostModifier,
                    enterTransition = {
                        fadeIn(animationSpec = tween(NavigationMotion.DurationMillis)) +
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(NavigationMotion.DurationMillis),
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(NavigationMotion.DurationMillis)) +
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(NavigationMotion.DurationMillis),
                            )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(NavigationMotion.DurationMillis)) +
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(NavigationMotion.DurationMillis),
                            )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(NavigationMotion.DurationMillis)) +
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(NavigationMotion.DurationMillis),
                            )
                    },
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
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable<HomeDestination> {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onDailyPickClick = { item -> navigateForItem(navController, item.id, item.type) },
                    onQuickActionClick = { action ->
                        when (action) {
                            HomeQuickAction.LikedSongs -> navController.navigate(LikedSongsDestination)
                            HomeQuickAction.RecentlyPlayed -> navController.navigate(RecentlyPlayedDestination)
                            HomeQuickAction.MyPlaylists -> navController.navigate(PlaylistsDestination)
                            HomeQuickAction.TopArtists -> navController.navigate(ArtistsDestination)
                        }
                    },
                    onMusicItemClick = { item -> navigateForItem(navController, item.id, item.type) },
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
                PlaylistsDestinationContent(navController)
            }
            composable<ProfileDestination> { entry ->
                val viewModel: ProfileViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val profileUpdated by entry.savedStateHandle
                    .getStateFlow(ProfileUpdatedResultKey, false)
                    .collectAsStateWithLifecycle()
                LaunchedEffect(profileUpdated) {
                    if (profileUpdated) {
                        entry.savedStateHandle[ProfileUpdatedResultKey] = false
                        viewModel.retry()
                        snackbarHostState.showSnackbar(profileSavedMessage)
                    }
                }
                ProfileScreen(
                    uiState = uiState,
                    onEditProfileClick = { navController.navigate(EditProfileDestination) },
                    onEntryClick = { entry ->
                        when (entry) {
                            ProfileEntry.Followers -> uiState.profile?.id?.let { id ->
                                navController.navigate(FollowListDestination(id, FollowListType.Followers))
                            }
                            ProfileEntry.Following -> uiState.profile?.id?.let { id ->
                                navController.navigate(FollowListDestination(id, FollowListType.Following))
                            }
                            ProfileEntry.LikedSongs -> navController.navigate(LikedSongsDestination)
                            ProfileEntry.RecentlyPlayed -> navController.navigate(RecentlyPlayedDestination)
                            ProfileEntry.Settings -> navController.navigate(SettingsDestination())
                            ProfileEntry.Chat -> navController.navigate(ChatListDestination)
                            ProfileEntry.Logout -> navController.navigate(SettingsDestination(showLogoutConfirmation = true))
                        }
                    },
                    onRetryClick = viewModel::retry,
                )
            }
            composable<PasswordRecoveryDestination> {
                val viewModel: PasswordRecoveryViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                PasswordRecoveryScreen(
                    email = uiState.email,
                    isSubmitted = uiState.isSubmitted,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onBackClick = { navController.popBackStack() },
                    onEmailChange = viewModel::updateEmail,
                    onSubmitClick = viewModel::submit,
                )
            }
            composable<EditProfileDestination> {
                val viewModel: ProfileEditorViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(uiState.isSaved) {
                    if (uiState.isSaved) {
                        shellAvatarUrl = uiState.profile?.avatarUrl
                        navController.previousBackStackEntry?.savedStateHandle?.set(ProfileUpdatedResultKey, true)
                        navController.popBackStack()
                    }
                }
                LaunchedEffect(uiState.shouldNavigateBack) {
                    if (uiState.shouldNavigateBack) {
                        viewModel.onIntent(ProfileEditorIntent.BackNavigationConsumed)
                        navController.popBackStack()
                    }
                }
                ProfileEditorScreen(
                    uiState = uiState,
                    onBackClick = { viewModel.onIntent(ProfileEditorIntent.BackRequested) },
                    onDisplayNameChange = { viewModel.onIntent(ProfileEditorIntent.DisplayNameChanged(it)) },
                    onUsernameChange = { viewModel.onIntent(ProfileEditorIntent.UsernameChanged(it)) },
                    onAvatarSelected = { viewModel.onIntent(ProfileEditorIntent.AvatarSelected(it)) },
                    onDeleteAvatarClick = { viewModel.onIntent(ProfileEditorIntent.DeleteAvatar) },
                    onSaveClick = { viewModel.onIntent(ProfileEditorIntent.Save) },
                    onRetryClick = { viewModel.onIntent(ProfileEditorIntent.Retry) },
                    onDiscardChangesClick = { viewModel.onIntent(ProfileEditorIntent.DiscardChanges) },
                    onDismissDiscardClick = { viewModel.onIntent(ProfileEditorIntent.DismissDiscardConfirmation) },
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
                    onPlaylistClick = { navController.navigate(PlaylistDestination(it.id)) },
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
                    onDownloadClick = {
                        if (uiState.isPremiumUser) viewModel.download() else navController.navigate(PremiumDestination)
                    },
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
                    onSongMoreClick = { songActionTarget = it },
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
                    onSongMoreClick = { songActionTarget = it },
                    onRetryClick = { viewModel.onIntent(ArtistDetailsIntent.Retry) },
                )
            }
            composable<SettingsDestination> { entry ->
                val args = entry.toRoute<SettingsDestination>()
                val viewModel: SettingsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(args.showLogoutConfirmation) {
                    if (args.showLogoutConfirmation) {
                        viewModel.onIntent(SettingsIntent.ShowLogoutConfirmation)
                    }
                }
                LaunchedEffect(uiState.isLogoutComplete) {
                    if (uiState.isLogoutComplete) {
                        navController.navigate(WelcomeDestination) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
                SettingsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onThemeClick = { viewModel.onIntent(SettingsIntent.ShowThemeOptions) },
                    onLanguageClick = { viewModel.onIntent(SettingsIntent.ShowLanguageOptions) },
                    onFontSizeClick = { viewModel.onIntent(SettingsIntent.ShowFontSizeOptions) },
                    onLogoutClick = { viewModel.onIntent(SettingsIntent.ShowLogoutConfirmation) },
                    onLogoutConfirm = { viewModel.onIntent(SettingsIntent.ConfirmLogout) },
                    onLogoutDismiss = { viewModel.onIntent(SettingsIntent.DismissLogoutConfirmation) },
                    onThemeSelected = { viewModel.onIntent(SettingsIntent.ThemeSelected(it)) },
                    onLanguageSelected = { viewModel.onIntent(SettingsIntent.LanguageSelected(it)) },
                    onFontScaleSelected = { viewModel.onIntent(SettingsIntent.FontScaleSelected(it)) },
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
                    onSongMoreClick = { songActionTarget = it },
                    onOverlayDismiss = { playerViewModel.onIntent(PlayerIntent.DismissOverlay) },
                    onSleepTimerSelected = { playerViewModel.onIntent(PlayerIntent.SleepTimerSelected(it)) },
                    onPlaybackSpeedSelected = { playerViewModel.onIntent(PlayerIntent.PlaybackSpeedSelected(it)) },
                    artworkModifier = Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(
                            "player-artwork-${playerUiState.currentSong?.id}",
                        ),
                        animatedVisibilityScope = this@AnimatedContent,
                    ),
                )
            }
            composable<LikedSongsDestination> {
                val viewModel: LikedSongsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LikedSongsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onSongMoreClick = { songActionTarget = it },
                    onRetryClick = { viewModel.onIntent(SongCollectionIntent.Retry) },
                    onPlayAllClick = { playerViewModel.onIntent(PlayerIntent.PlayQueue(uiState.songs)) },
                    onShuffleClick = { playerViewModel.onIntent(PlayerIntent.PlayQueue(uiState.songs)); playerViewModel.onIntent(PlayerIntent.ToggleShuffle) },
                    onRemoveClick = { viewModel.onIntent(SongCollectionIntent.Remove(it.id)) },
                )
            }
            composable<RecentlyPlayedDestination> {
                val viewModel: RecentlyPlayedViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                RecentlyPlayedScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onSongMoreClick = { songActionTarget = it },
                    onRetryClick = { viewModel.onIntent(SongCollectionIntent.Retry) },
                    onPlayAllClick = { playerViewModel.onIntent(PlayerIntent.PlayQueue(uiState.songs)) },
                    onShuffleClick = { playerViewModel.onIntent(PlayerIntent.PlayQueue(uiState.songs)); playerViewModel.onIntent(PlayerIntent.ToggleShuffle) },
                    onRemoveClick = { viewModel.onIntent(SongCollectionIntent.Remove(it.id)) },
                )
            }
            composable<ArtistsDestination> {
                val viewModel: ArtistsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ArtistsScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
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
                    onNotificationClick = { notification ->
                        viewModel.onIntent(NotificationsIntent.NotificationSelected(notification))
                        when (val target = notification.target) {
                            is NotificationTarget.Song -> navController.navigate(SongDestination(target.songId))
                            is NotificationTarget.Playlist -> navController.navigate(PlaylistDestination(target.playlistId))
                            is NotificationTarget.Artist -> navController.navigate(ArtistDestination(target.artistId))
                            is NotificationTarget.UserProfile -> navController.navigate(UserProfileDestination(target.userId))
                            is NotificationTarget.Conversation -> navController.navigate(
                                ChatDetailDestination(
                                    conversationId = target.conversationId,
                                    participantId = target.participantId,
                                    participantUsername = target.participantUsername,
                                    participantDisplayName = target.participantDisplayName,
                                    participantAvatarUrl = target.participantAvatarUrl,
                                ),
                            )
                            NotificationTarget.Premium -> navController.navigate(PremiumDestination)
                            null -> unavailableAction(notificationTargetUnavailableMessage)
                        }
                    },
                    onMarkAllReadClick = { viewModel.onIntent(NotificationsIntent.MarkAllRead) },
                    onRetryClick = { viewModel.onIntent(NotificationsIntent.Retry) },
                    onBackClick = { navController.popBackStack() },
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
                    onBackClick = { navController.popBackStack() },
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
                    onShareSongClick = {
                        playerUiState.currentSong?.let { song ->
                            navController.navigate(ChatPickerDestination(song.id))
                        } ?: unavailableAction(unavailableMessage)
                    },
                    onSongClick = { navController.navigate(SongDestination(it.id)) },
                    onRetryClick = { viewModel.onIntent(ChatDetailIntent.Retry) },
                    onVisibleUnreadMessages = { messages ->
                        viewModel.onIntent(ChatDetailIntent.MarkMessagesRead(messages.map(ChatMessage::id)))
                    },
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
                LaunchedEffect(args.userId, args.type) {
                    viewModel.onIntent(FollowListIntent.Load(args.userId, args.type))
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
    songActionTarget?.let { song ->
        SongActionSheet(
            song = song,
            onDismiss = { songActionTarget = null },
            onPlayClick = {
                playerViewModel.onIntent(PlayerIntent.Play(song))
                songActionTarget = null
            },
            onAddToPlaylistClick = {
                songActionTarget = null
                navController.navigate(AddToPlaylistDestination(song.id))
            },
            onShareClick = {
                songActionTarget = null
                navController.navigate(ChatPickerDestination(song.id))
            },
        )
    }
    }
}

@Composable
private fun SessionRestoreScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlaylistsDestinationContent(navController: NavHostController) {
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

private object NavigationMotion {
    const val DurationMillis = 220
}

private fun navigateForItem(controller: NavHostController, id: String, type: MusicItemType) {
    when (type) {
        MusicItemType.Playlist -> controller.navigate(PlaylistDestination(id))
        MusicItemType.Artist -> controller.navigate(ArtistDestination(id))
        MusicItemType.Song -> controller.navigate(SongDestination(id))
        // Album details are not yet a product destination. Do not misroute the card to a song.
        MusicItemType.Album -> Unit
    }
}
