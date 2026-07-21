package com.androidprj.fuzic.model

import androidx.annotation.StringRes
import com.androidprj.fuzic.R

data class PlaylistsUiState(
    val sections: List<PlaylistSection> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createPlaylistState: CreatePlaylistUiState = CreatePlaylistUiState()
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && sections.all { it.playlists.isEmpty() }
}

data class PlaylistSection(
    @StringRes val titleRes: Int,
    val type: PlaylistSectionType,
    val playlists: List<PlaylistItem>
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val songCountLabel: String,
    val artworkUrl: String? = null
)

data class CreatePlaylistUiState(
    val isVisible: Boolean = false,
    val name: String = "",
    val hasNameConflict: Boolean = false,
    val selectedCoverUri: String? = null,
    val availableCoverUris: List<String> = emptyList(),
)

enum class PlaylistSectionType {
    Global,
    Local,
    Mine
}

enum class PlaylistGradient(@StringRes val labelRes: Int) {
    Violet(R.string.playlists_gradient_violet),
    Teal(R.string.playlists_gradient_teal),
    Gold(R.string.playlists_gradient_gold)
}
