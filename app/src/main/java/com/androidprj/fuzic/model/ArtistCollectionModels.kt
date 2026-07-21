package com.androidprj.fuzic.model

data class ArtistCollectionUiState(
    val artists: List<ArtistCollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && artists.isEmpty()
}

data class ArtistCollectionItem(
    val artist: ArtistItem,
    val followersLabel: String,
    val isFollowing: Boolean = false,
)
