package com.androidprj.fuzic.model.ui

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val album: String? = null,
    val durationLabel: String? = null,
    val isExplicit: Boolean = false,
    /** Stream/download source resolved by the catalog repository; never a storage implementation detail. */
    val audioUrl: String? = null,
)

data class SongDetailsUiState(
    val song: SongItem? = null,
    val isLiked: Boolean = false,
    val isPremiumUser: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ArtistDetailsUiState(
    val artist: ArtistItem? = null,
    val popularSongs: List<SongItem> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && popularSongs.isEmpty()
}

data class ArtistItem(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val monthlyListenersLabel: String? = null,
)

data class PlaylistDetailsUiState(
    val playlist: PlaylistDetails? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class PlaylistDetails(
    val id: String,
    val title: String,
    val description: String,
    val artworkUrl: String? = null,
    val ownerName: String,
    val songs: List<SongItem> = emptyList(),
)

data class SongCollectionUiState(
    val title: String,
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && songs.isEmpty()
}
