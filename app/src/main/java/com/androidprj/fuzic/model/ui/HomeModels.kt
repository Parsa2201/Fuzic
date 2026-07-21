package com.androidprj.fuzic.model.ui

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class HomeUiState(
    val isLoading: Boolean = false,
    val dailyPicks: List<FeaturedMusicItem> = emptyList(),
    val quickActions: List<HomeQuickAction> = HomeQuickAction.defaults,
    val sections: List<HomeMusicSection> = emptyList(),
    val isShowingCachedContent: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && dailyPicks.isEmpty() && sections.all { it.items.isEmpty() }
}

data class FeaturedMusicItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val type: MusicItemType = MusicItemType.Song
)

data class HomeMusicSection(
    @StringRes val titleRes: Int,
    val items: List<FeaturedMusicItem>
)

enum class MusicItemType {
    Song,
    Album,
    Playlist,
    Artist
}

enum class HomeQuickAction(@StringRes val labelRes: Int) {
    LikedSongs(R.string.home_quick_action_liked_songs),
    RecentlyPlayed(R.string.home_quick_action_recently_played),
    MyPlaylists(R.string.home_quick_action_my_playlists),
    TopArtists(R.string.home_quick_action_top_artists);

    companion object {
        val defaults = entries
    }
}
