package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.model.ui.DownloadRequest
import com.androidprj.fuzic.model.ui.DownloadSortOption
import com.androidprj.fuzic.model.ui.DownloadedSongItem
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.PlaylistDetails
import com.androidprj.fuzic.model.ui.PremiumPlan
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.AudioVisualizerFrame
import com.androidprj.fuzic.model.ui.ArtistDetails
import com.androidprj.fuzic.model.ui.ArtistItem
import com.androidprj.fuzic.model.ui.PlayerUiState
import com.androidprj.fuzic.model.ui.NotificationItem
import com.androidprj.fuzic.model.ui.NotificationType
import com.androidprj.fuzic.model.ui.RepeatMode
import com.androidprj.fuzic.model.ui.SearchFilter
import com.androidprj.fuzic.model.ui.SearchResultItem
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.DownloadRepository
import com.androidprj.fuzic.repository.ArtistRepository
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.repository.MusicRepository
import com.androidprj.fuzic.repository.NotificationRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.PlayerRepository
import com.androidprj.fuzic.repository.PlaylistDetailsRepository
import com.androidprj.fuzic.repository.PremiumRepository
import com.androidprj.fuzic.repository.SearchRepository
import com.androidprj.fuzic.repository.SettingsRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.util.StringProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal val testSong = SongItem(
    id = "song-1",
    title = "Midnight Drive",
    artist = "Luna Ray",
)

internal val testPlaylist = PlaylistItem(
    id = "playlist-1",
    title = "Evening Mix",
    subtitle = "Parsa",
    songCountLabel = "10 songs",
)

internal val testPlaylistDetails = PlaylistDetails(
    id = "playlist-1",
    title = "Evening Mix",
    description = "Night tracks",
    ownerName = "Parsa",
    songs = listOf(testSong),
)

internal val testProfile = ProfileUser(
    id = "user-1",
    username = "parsa",
    displayName = "Parsa",
)

internal val testArtist = ArtistItem(
    id = "artist-1",
    name = "Luna Ray",
    monthlyListenersLabel = "1M listeners",
)

internal val testArtistDetails = ArtistDetails(
    artist = testArtist,
    popularSongs = listOf(testSong),
)

internal val testPremiumPlan = PremiumPlan(
    id = "premium-monthly",
    title = "Monthly",
    priceLabel = "$4.99",
    billingLabel = "Monthly",
    isRecommended = true,
)

internal val testDownload = DownloadedSongItem(
    id = "download-1",
    title = "Midnight Drive",
    artist = "Luna Ray",
    fileSizeLabel = "8 MB",
    downloadedAtLabel = "Today",
)

internal val testSearchResult = SearchResultItem(
    id = "search-1",
    title = "Midnight Drive",
    subtitle = "Luna Ray",
    type = SearchFilter.Songs,
)

internal val testNotification = NotificationItem(
    id = "notification-1",
    title = "New follower",
    message = "Nika followed you",
    timeLabel = "Now",
    type = NotificationType.Follow,
    isRead = false,
)

internal object FakeStringProvider : StringProvider {
    override fun get(resourceId: Int): String = "localized-$resourceId"
}

internal class FakeAuthRepository(
    var currentUserId: String? = "user-1",
    var loginResult: Result<Unit> = Result.success(Unit),
    var signUpResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    var loginCalls = 0
    var signUpCalls = 0
    var logoutCalls = 0
    var lastSignUpName: String? = null

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls++
        return loginResult
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<Unit> {
        signUpCalls++
        lastSignUpName = name
        return signUpResult
    }

    var logoutResult: Result<Unit> = Result.success(Unit)

    override suspend fun logout(): Result<Unit> {
        logoutCalls++
        return logoutResult
    }

    override fun getCurrentUserFlow(): Flow<ProfileUser?> = flowOf(testProfile)

    override suspend fun getCurrentUserId(): String? = currentUserId
}

internal class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings(),
) : SettingsRepository {
    val settings = MutableStateFlow(initialSettings)
    var themeResult: Result<Unit> = Result.success(Unit)
    var languageResult: Result<Unit> = Result.success(Unit)
    var clearResult: Result<Unit> = Result.success(Unit)
    var setThemeCalls = 0
    var setLanguageCalls = 0

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun setTheme(theme: AppThemeOption): Result<Unit> {
        setThemeCalls++
        return themeResult.onSuccess { settings.value = settings.value.copy(theme = theme) }
    }

    override suspend fun setLanguage(language: AppLanguageOption): Result<Unit> {
        setLanguageCalls++
        return languageResult.onSuccess { settings.value = settings.value.copy(language = language) }
    }

    override suspend fun clearSettings(): Result<Unit> = clearResult
}

internal class FakePremiumRepository(
    initialPremium: Boolean = false,
    var plansResult: Result<List<PremiumPlan>> = Result.success(listOf(testPremiumPlan)),
) : PremiumRepository {
    val premiumStatus = MutableStateFlow(initialPremium)
    var purchaseResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<Unit> = Result.success(Unit)
    var purchaseCalls = 0
    var restoreCalls = 0
    var lastPurchasedPlanId: String? = null

    override fun observePremiumStatus(): Flow<Boolean> = premiumStatus

    override suspend fun getPlans(): Result<List<PremiumPlan>> = plansResult

    override suspend fun purchasePlan(planId: String): Result<Unit> {
        purchaseCalls++
        lastPurchasedPlanId = planId
        return purchaseResult.onSuccess { premiumStatus.value = true }
    }

    override suspend fun restorePurchase(): Result<Unit> {
        restoreCalls++
        return restoreResult
    }
}

internal class FakeDownloadRepository(
    initialDownloads: List<DownloadedSongItem> = listOf(testDownload),
) : DownloadRepository {
    val downloads = MutableStateFlow(initialDownloads)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<Unit> = Result.success(Unit)
    var removeFileResult: Result<Unit> = Result.success(Unit)
    var deleteCalls = 0
    var restoreCalls = 0
    var removeFileCalls = 0
    var observedSortOption: DownloadSortOption? = null

    override fun observeDownloads(sortOption: DownloadSortOption): Flow<List<DownloadedSongItem>> {
        observedSortOption = sortOption
        return downloads
    }

    override suspend fun enqueueDownload(request: DownloadRequest): Result<Unit> = Result.success(Unit)

    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        deleteCalls++
        return deleteResult.onSuccess { downloads.value = downloads.value.filterNot { it.id == downloadId } }
    }

    override suspend fun restoreDownload(downloadId: String): Result<Unit> {
        restoreCalls++
        return restoreResult
    }

    override suspend fun removeDownloadFile(downloadId: String): Result<Unit> {
        removeFileCalls++
        return removeFileResult
    }
}

internal class FakePlayerRepository(
    initialState: PlayerUiState = PlayerUiState(),
) : PlayerRepository {
    private val _playerState = MutableStateFlow(initialState)
    override val playerState: StateFlow<PlayerUiState> = _playerState
    override val visualizerFrames = MutableSharedFlow<AudioVisualizerFrame>()
    var commandResult: Result<Unit> = Result.success(Unit)
    var playCalls = 0
    var toggleCalls = 0
    var nextCalls = 0
    var previousCalls = 0
    var seekCalls = 0
    var lastSeekProgress: Float? = null
    var lastRepeatMode: RepeatMode? = null
    var lastShuffleEnabled: Boolean? = null
    var lastSleepTimer: Int? = null
    var lastSpeed: Float? = null

    override suspend fun play(song: SongItem): Result<Unit> {
        playCalls++
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(currentSong = song, isPlaying = true) }
    }

    override suspend fun playQueue(songs: List<SongItem>, startIndex: Int): Result<Unit> =
        commandResult.onSuccess { _playerState.value = _playerState.value.copy(currentSong = songs.getOrNull(startIndex), queue = songs) }

    override suspend fun togglePlayPause(): Result<Unit> {
        toggleCalls++
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(isPlaying = !_playerState.value.isPlaying) }
    }

    override suspend fun seekTo(progress: Float): Result<Unit> {
        seekCalls++
        lastSeekProgress = progress
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(progress = progress) }
    }

    override suspend fun skipToPrevious(): Result<Unit> {
        previousCalls++
        return commandResult
    }

    override suspend fun skipToNext(): Result<Unit> {
        nextCalls++
        return commandResult
    }

    override suspend fun setShuffleEnabled(enabled: Boolean): Result<Unit> {
        lastShuffleEnabled = enabled
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(isShuffleEnabled = enabled) }
    }

    override suspend fun setRepeatMode(mode: RepeatMode): Result<Unit> {
        lastRepeatMode = mode
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(repeatMode = mode) }
    }

    override suspend fun setPlaybackSpeed(speed: Float): Result<Unit> {
        lastSpeed = speed
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(playbackSpeed = speed) }
    }

    override suspend fun setSleepTimer(minutes: Int?): Result<Unit> {
        lastSleepTimer = minutes
        return commandResult.onSuccess { _playerState.value = _playerState.value.copy(sleepTimerMinutes = minutes) }
    }

    override suspend fun addToQueue(song: SongItem): Result<Unit> = commandResult
    override suspend fun removeFromQueue(songId: String): Result<Unit> = commandResult
    override suspend fun clearQueue(): Result<Unit> = commandResult
    override suspend fun stop(): Result<Unit> = commandResult.onSuccess { _playerState.value = PlayerUiState() }
}

internal class FakeMusicRepository(
    var dailyResult: Result<List<SongItem>> = Result.success(listOf(testSong)),
    var popularResult: Result<List<SongItem>> = Result.success(listOf(testSong)),
    var releasesResult: Result<List<SongItem>> = Result.success(listOf(testSong)),
    var songResult: Result<SongItem> = Result.success(testSong),
) : MusicRepository {
    override suspend fun getDailyPicks(offset: Long, limit: Long) = dailyResult
    override suspend fun getTrendingSongs(offset: Long, limit: Long) = popularResult
    override suspend fun getNewReleases(offset: Long, limit: Long) = releasesResult
    override suspend fun getMostPopular(offset: Long, limit: Long) = popularResult
    override suspend fun getSongById(songId: String) = songResult
    override suspend fun searchSongs(query: String, offset: Long, limit: Long) =
        Result.success(emptyList<SongItem>())
}

internal class FakePlaylistRepository(
    var globalResult: Result<List<PlaylistItem>> = Result.success(listOf(testPlaylist)),
    var localResult: Result<List<PlaylistItem>> = Result.success(listOf(testPlaylist.copy(id = "local-1"))),
    var userResult: Result<List<PlaylistItem>> = Result.success(listOf(testPlaylist)),
) : PlaylistRepository {
    var createCalls = 0
    var lastCreatedTitle: String? = null
    var lastCreateRequest: CreatePlaylistRequest? = null

    override suspend fun getGlobalPlaylists(offset: Long, limit: Long) = globalResult
    override suspend fun getLocalPlaylists(offset: Long, limit: Long) = localResult
    override suspend fun getUserPlaylists(userId: String, offset: Long, limit: Long) = userResult
    override suspend fun getPlaylistSongs(playlistId: String, offset: Long, limit: Long) = Result.success(listOf(testSong))

    override suspend fun createPlaylist(request: CreatePlaylistRequest): Result<PlaylistItem> {
        createCalls++
        lastCreateRequest = request
        lastCreatedTitle = request.title
        return Result.success(testPlaylist.copy(title = request.title))
    }

    override suspend fun deletePlaylist(playlistId: String) = Result.success(Unit)
    override suspend fun addSongToPlaylist(playlistId: String, songId: String) = Result.success(Unit)
    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = Result.success(Unit)
}

internal class FakePlaylistDetailsRepository(
    var detailsResult: Result<PlaylistDetails> = Result.success(testPlaylistDetails),
) : PlaylistDetailsRepository {
    var loadCalls = 0
    var lastPlaylistId: String? = null

    override suspend fun getPlaylistDetails(playlistId: String): Result<PlaylistDetails> {
        loadCalls++
        lastPlaylistId = playlistId
        return detailsResult
    }
}

internal class FakeArtistRepository(
    var artistDetailsResult: Result<ArtistDetails> = Result.success(testArtistDetails),
) : ArtistRepository {
    var detailsCalls = 0
    var lastArtistId: String? = null

    override suspend fun getArtist(artistId: String): Result<ArtistItem> = artistDetailsResult.map { it.artist }

    override suspend fun getArtistDetails(artistId: String): Result<ArtistDetails> {
        detailsCalls++
        lastArtistId = artistId
        return artistDetailsResult
    }

    override fun observeArtists(): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<com.androidprj.fuzic.model.ui.ArtistCollectionItem>> {
        return flowOf(androidx.paging.PagingData.empty())
    }
}

internal class FakeUserRepository(
    var profileResult: Result<ProfileUser> = Result.success(testProfile),
) : UserRepository {
    override suspend fun getUserProfile(userId: String) = profileResult
    override suspend fun updateProfile(user: ProfileUser) = Result.success(user)
    override suspend fun searchUsers(query: String) =
        Result.success(emptyList<ProfileUser>())
}

internal class FakeFollowRepository(
    var followersCount: Int = 7,
    var followingCount: Int = 3,
) : com.androidprj.fuzic.repository.FollowRepository {
    var followResult: Result<Unit> = Result.success(Unit)
    var unfollowResult: Result<Unit> = Result.success(Unit)
    var followCalls = 0
    var unfollowCalls = 0

    override suspend fun followUser(followeeId: String): Result<Unit> {
        followCalls++
        return followResult
    }
    override suspend fun unfollowUser(followeeId: String): Result<Unit> {
        unfollowCalls++
        return unfollowResult
    }
    override suspend fun getFollowers(userId: String, offset: Long, limit: Long) = Result.success(emptyList<com.androidprj.fuzic.model.ui.FollowUser>())
    override suspend fun getFollowing(userId: String, offset: Long, limit: Long) = Result.success(emptyList<com.androidprj.fuzic.model.ui.FollowUser>())
    override fun observeFollowersCount(userId: String): Flow<Int> = flowOf(followersCount)
    override fun observeFollowingCount(userId: String): Flow<Int> = flowOf(followingCount)
}

internal class FakeInteractionRepository(
    var likeResult: Result<Unit> = Result.success(Unit),
) : InteractionRepository {
    var likeCalls = 0
    var unlikeCalls = 0

    override suspend fun getRecentlyPlayed(userId: String, offset: Long, limit: Long) = Result.success(emptyList<SongItem>())
    override suspend fun getLikedSongs(userId: String, offset: Long, limit: Long) = Result.success(emptyList<SongItem>())
    override suspend fun recordPlay(songId: String) = Result.success(Unit)
    override suspend fun likeSong(songId: String): Result<Unit> {
        likeCalls++
        return likeResult
    }
    override suspend fun unlikeSong(songId: String): Result<Unit> {
        unlikeCalls++
        return Result.success(Unit)
    }
}

internal class FakeSearchRepository(
    initialHistory: List<String> = emptyList(),
) : SearchRepository {
    val history = MutableStateFlow(initialHistory)
    var searchThrows: Throwable? = null
    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var clearResult: Result<Unit> = Result.success(Unit)
    var searchCalls = 0
    var lastQuery: String? = null
    var lastFilter: SearchFilter? = null

    override fun search(
        query: String,
        filter: SearchFilter
    ): Flow<androidx.paging.PagingData<SearchResultItem>> {
        searchCalls++
        lastQuery = query
        lastFilter = filter
        searchThrows?.let { throw it }
        return flowOf(androidx.paging.PagingData.empty())
    }

    override fun observeSearchHistory(): Flow<List<String>> = history

    override suspend fun saveSearchQuery(query: String): Result<Unit> {
        return saveResult.onSuccess { history.value = listOf(query) + history.value.filterNot { it == query } }
    }

    override suspend fun deleteSearchQuery(query: String): Result<Unit> {
        return deleteResult.onSuccess { history.value = history.value.filterNot { it == query } }
    }

    override suspend fun clearSearchHistory(): Result<Unit> {
        return clearResult.onSuccess { history.value = emptyList() }
    }
}

internal class FakeNotificationRepository : NotificationRepository {
    var markResult: Result<Unit> = Result.success(Unit)
    var markAllResult: Result<Unit> = Result.success(Unit)
    var markCalls = 0
    var markAllCalls = 0

    override fun observeNotifications(): Flow<androidx.paging.PagingData<NotificationItem>> {
        return flowOf(androidx.paging.PagingData.empty())
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        markCalls++
        return markResult
    }

    override suspend fun markAllNotificationsAsRead(): Result<Unit> {
        markAllCalls++
        return markAllResult
    }
}
