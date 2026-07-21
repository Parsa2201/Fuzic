package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class ProfileUiState(
    val profile: ProfileUser? = null,
    val stats: ProfileStats = ProfileStats(),
    val entries: List<ProfileEntry> = ProfileEntry.defaults,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ProfileUser(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false
)

data class ProfileStats(
    val followersLabel: String = "0",
    val followingLabel: String = "0",
    val playlistsLabel: String = "0"
)

enum class ProfileEntry(@StringRes val labelRes: Int) {
    Followers(R.string.profile_entry_followers),
    Following(R.string.profile_entry_following),
    LikedSongs(R.string.profile_entry_liked_songs),
    RecentlyPlayed(R.string.profile_entry_recently_played),
    Settings(R.string.profile_entry_settings),
    Chat(R.string.profile_entry_chat),
    Logout(R.string.profile_entry_logout);

    companion object {
        val defaults = entries
    }
}

enum class ProfileStat(@StringRes val labelRes: Int) {
    Followers(R.string.profile_stat_followers),
    Following(R.string.profile_stat_following),
    Playlists(R.string.profile_stat_playlists)
}
