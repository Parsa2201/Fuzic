package com.androidprj.fuzic

import com.androidprj.fuzic.model.ui.CreatePlaylistRequest
import com.androidprj.fuzic.model.ui.AppLanguageOption
import com.androidprj.fuzic.model.ui.AppSettings
import com.androidprj.fuzic.model.ui.AppThemeOption
import com.androidprj.fuzic.model.ui.PlaylistItem
import com.androidprj.fuzic.model.ui.ProfileUser
import com.androidprj.fuzic.model.ui.SongItem
import com.androidprj.fuzic.repository.AuthRepository
import com.androidprj.fuzic.repository.InteractionRepository
import com.androidprj.fuzic.repository.MusicRepository
import com.androidprj.fuzic.repository.PlaylistRepository
import com.androidprj.fuzic.repository.SettingsRepository
import com.androidprj.fuzic.repository.UserRepository
import com.androidprj.fuzic.util.StringProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

internal val testProfile = ProfileUser(
    id = "user-1",
    username = "parsa",
    displayName = "Parsa",
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
    override suspend fun followUser(followeeId: String) = Result.success(Unit)
    override suspend fun unfollowUser(followeeId: String) = Result.success(Unit)
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
